package dev.mdklatt.idea.netcdf.viewer

import com.intellij.openapi.diagnostic.Logger
import java.lang.IllegalStateException
import javax.swing.table.AbstractTableModel
import kotlin.math.ceil
import ucar.nc2.Dimension
import ucar.nc2.NetcdfFile
import ucar.nc2.Variable
import vendor.tandrial.itertools.cartProd
import dev.mdklatt.idea.netcdf.*


/**
 * Read n-dimensional netCDF data as a two-dimensional table where variables
 * are mapped to columns in flattened row-major order.
 */
internal class NetcdfReader(private val file: NetcdfFile, variables: Sequence<String>) {

    /**
     * Define a table column.
     */
    abstract class Column(val label: String) {
        /**
         * Data type represented by this column.
         */
        open val type : Class<*> = Any::class.java

        /**
         * Get a single column value.
         *
         * @param row: row index
         * @return: column value
         */
        abstract fun value(row: Int) : Any
    }

    /**
     * Table column holding integer indexes for a dimension.
     *
     * @param label: column label
     * @param axis: axis index corresponding to the dimension
     */
    inner class IndexColumn(label: String, private val axis: Int) : Column(label) {

        override val type = Int::class.java

        /**
         * Get a single column value.
         *
         * @param row: row index
         * @return: column value
         */
        override fun value(row: Int) = index[row][axis]
    }

    /**
     * Table column holding a netCDF variable.
     *
     * @param variable: netCDF variable
     */
    open inner class VariableColumn(private val variable: Variable) : Column(variable.fullName) {
        /** @see Column.type */
        override val type: Class<*> = variable.dataType.primitiveClassType

        /**
         * Mapping of variable axes to table axes.
         */
        private val axes: IntArray = variable.publicDimensions.map {
            val index = dimensions.indexOf(it)
            if (index != -1) index else throw IllegalStateException("incompatible dimensions")
        }.toIntArray()

        /**
         * Shape required to read a variable element.
         */
        protected open val shape = IntArray(variable.dimensions.size) { 1 }

        /**
         * Translate a data space index to an origin point for this variable.
         * This is used in conjunction with `shape` to define the array section
         * required to read a single value from the variable.
         */
        protected open fun origin(index: IntArray) = index.slice(axes.asIterable()).toIntArray()

        /**
         * Read a value from the underlying netCDF variable. Child classes
         * should override this if their column value is composed of multiple
         * variable values.
         *
         * @param index: data space index
         * @return: variables values(s)
         */
        protected open fun read(index: IntArray) : ucar.ma2.Array = variable.read(origin(index), shape)

        /**
         * Get the variable value for the given table row.
         *
         * @param row row index
         * @return variable value
         */
        override fun value(row: Int) : Any = read(index[row]).getObject(0)
    }

    /**
     * Column of fixed-length "classic" strings.
     *
     * @see <a href=http://www.bic.mni.mcgill.ca/users/sean/Docs/netcdf/guide.txn_58.html>Reading and Writing Character String Values</a>
     */
    inner class ArrayStringColumn(variable: Variable) : VariableColumn(variable) {

        private val strLength = variable.shape.last()

        /** @see VariableColumn.type */
        override val type = String::class.java

        /** @see VariableColumn.shape */
        // Override base class to account for additional string length dimension.
        override val shape = IntArray(variable.shape.size - 1) { 1 } + intArrayOf(strLength)

        /** @see VariableColumn.origin */
        // Override base class to account for additional string length dimension.
        override fun origin(index: IntArray) = super.origin(index) + intArrayOf(0)

        /** @see VariableColumn.value */
        // Override base class to convert a character array to a String.
        override fun value(row: Int) = read(index[row]).toString()
    }

    /**
     * Column of netCDF time values.
     *
     * @see <a href="https://www.unidata.ucar.edu/software/netcdf/time/recs.html">A Brief History of (netCDF) Time<a>
     */
    inner class TimeColumn(variable: Variable) : VariableColumn(variable) {
        /** @see VariableColumn.type */
        override val type = String::class.java

        private val units = variable.dateUnits ?: throw IllegalStateException("Not a valid time variable")

        /** @see VariableColumn.read */
        override fun value(row: Int) : Any {
            // Override base class to convert numeric time offsets to ISO 8601
            // timestamps.
            val dataValue = read(index[row])
            val timeValue = if (dataValue.dataType.isIntegral) {
                units.makeCalendarDate(dataValue.getObject(0).toString().toInt())
            } else {
                units.makeCalendarDate(dataValue.getObject(0).toString().toDouble())
            }
            return timeValue.toString()
        }
    }

    private val logger = Logger.getInstance(this::class.java)
    private var dimensions = emptyList<Dimension>()
    private var index = emptyList<IntArray>()
    var columns = mutableListOf<Column>()

    val rowCount: Int
        get() = index.size

    init {
        variables.forEach {name ->
            logger.debug("Loading $name variable from ${file.location}")
            val variable = file.findVariable(name) ?: throw IllegalArgumentException("Invalid variable: $name")
            if (dimensions.isEmpty()) {
                dimensions = variable.publicDimensions
                dimensions.forEach { addCoordinateColumn(it) }
            } else if (!isCongruent(variable)) {
                throw IllegalArgumentException("Cannot add incongruent variable '${name}' to table")
            }
            if (columns.find { it.label == name } == null) {
                // Exclude duplicates, including variables that are already
                // present as coordinate variables.
                addVariableColumn(variable)
            }
        }
        val shape = dimensions.map { (0 until it.length) }.toTypedArray()
        if (!shape.isEmpty()) {
            index = cartProd(*shape).map { it.toIntArray() }.toList()
        }
    }

    /**
     * Returns the value for the cell at `columnIndex` and `rowIndex`.
     *
     * @param rowIndex        the row whose value is to be queried
     * @param columnIndex     the column whose value is to be queried
     * @return value of the given table cell
     */
    fun getValueAt(rowIndex: Int, columnIndex: Int) =
        columns[columnIndex].value(rowIndex)

    /**
     * Test if a variable has the same dimensions as the current table
     * dimensions, irrespective of order.
     *
     * @return: true if the variable is compatible
     */
    private fun isCongruent(variable: Variable) : Boolean {
        return setOf(dimensions) == setOf(variable.publicDimensions)
    }

    /**
     * Add a coordinate variable column for a dimension to the table. If there
     * is no coordinate variable for that dimension, the column will be an
     * integer index. Coordinate columns are expected to come before any
     * regular variable columns.
     *
     * @param dimension: dimension to add a coordinate column for
     */
    private fun addCoordinateColumn(dimension: Dimension) {
        val variable = file.findVariable(dimension.fullName)
        if (variable?.isCoordinateVariable != true) {
            // No coordinate variable for this dimension.
            val axis = dimensions.indexOf(dimension)
            columns.add(IndexColumn(dimension.fullName, axis))
        } else {
            addVariableColumn(variable)
        }
    }

    /**
     * Add a regular variable column to the table.
     *
     * @param variable: variable to add a column for
     */
    private fun addVariableColumn(variable: Variable) {
        val column = if (variable.isTime) {
            TimeColumn(variable)
        } else if (variable.isArrayString) {
            ArrayStringColumn(variable)
        } else {
            VariableColumn(variable)
        }
        columns.add(column)
    }

}


/**
 * Represent n-dimensional netCDF variables as a two-dimensional table where
 * variables are mapped to columns in flattened row-major order.
*/
internal class DataModel(val pageSize: Int = 100) : AbstractTableModel() {

    private val logger = Logger.getInstance(this::class.java)
    private var file: NetcdfFile? = null
    private var reader: NetcdfReader? = null

    val labels : List<String>
        get() = reader?.columns?.map { it.label }?.toList() ?: emptyList()

    val pageCount: Int
        get() {
            val count = reader?.rowCount?.toDouble() ?: 0.0
            return ceil(count.div(pageSize)).toInt()
        }

    var pageNumber: Int = 0
        set(value) {
           field = if (pageCount == 0) 0 else value.coerceIn(1, pageCount)
        }

    /**
     * Fill the table with netCDF variables.
     *
     * The model defines columns consisting of one or more netCDF variables and
     * their corresponding dimension coordinates. All selected variable must
     * have congruent dimensions.
     *
     * @param file: open netCDF file
     * @param varNames: variable names to use
     */
    fun fillTable(file: NetcdfFile, varNames: Sequence<String>) {
        this.file = file
        reader = NetcdfReader(file, varNames)
        pageNumber = 1
        fireTableStructureChanged()
    }

    /**
     * Clear all columns from the table.
     */
    fun clearTable() {
        reader = null
        pageNumber = 0
        file = null
    }

    /**
     * Returns the number of rows in the model. A
     * `JTable` uses this method to determine how many rows it
     * should display.  This method should be quick, as it
     * is called frequently during rendering.
     *
     * @return the number of rows in the model
     * @see .getColumnCount
     */
    override fun getRowCount() = if (pageNumber == pageCount) {
        reader?.rowCount?.minus((pageCount - 1) * pageSize) ?: 0
    } else {
        pageSize
    }

    /**
     * Returns the number of columns in the model. A
     * `JTable` uses this method to determine how many columns it
     * should create and display by default.
     *
     * @return the number of columns in the model
     * @see .getRowCount
     */
    override fun getColumnCount() = labels.size

    /**
     * Get the type for a column.
     */
    override fun getColumnClass(columnIndex: Int): Class<*> =
        reader?.columns?.get(columnIndex)?.type ?: throw RuntimeException("Table is undefined")

    /**
     * Get the name label for a column.
     */
    override fun getColumnName(columnIndex: Int) =
        labels[columnIndex]

    /**
     * Returns the value for the cell at `columnIndex` and
     * `rowIndex`.
     *
     * @param   rowIndex        the row whose value is to be queried
     * @param   columnIndex     the column whose value is to be queried
     * @return  the value Object at the specified cell
     */
    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        val fileRow = (pageNumber - 1) * pageSize + rowIndex
        return reader?.columns?.get(columnIndex)?.value(fileRow)

    }
}
