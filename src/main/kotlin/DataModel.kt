package dev.mdklatt.idea.netcdf

import com.intellij.openapi.diagnostic.Logger
import java.lang.IllegalStateException
import javax.swing.table.AbstractTableModel
import ucar.nc2.Dimension
import ucar.nc2.NetcdfFile
import ucar.nc2.Variable
import vendor.tandrial.itertools.cartProd


/**
 * Represent n-dimensional netCDF variables as a two-dimensional table where
 * variables are mapped to columns in flattened row-major order.
*/
internal class DataModel : AbstractTableModel() {
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
     *
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
    open inner class VariableColumn(private val variable: Variable) : Column(variable.fullNameEscaped) {
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

        /** @see VariableColumn.value */
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

        private val units = variable.dateUnits ?: throw IllegalStateException("not a valid time variable")

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
    private var file: NetcdfFile? = null
    private var dimensions = emptyList<Dimension>()
    private var index : List<IntArray> = emptyList()
    private var columns = mutableListOf<Column>()

    val labels : List<String>
        get() = columns.map { it.label }.toList()


    /**
     * Set the model data.
     *
     * The model defines columns consisting of one or more netCDF variables and
     * their corresponding dimension coordinates. All selected variable must
     * have congruent dimensions.
     *
     * @param file: open netCDF file
     * @param varNames: variable names to use
     */
    fun setData(file: NetcdfFile, varNames: Sequence<String>) {
        logger.debug("Loading data from ${file.location}")
        resetData()
        this.file = file
        varNames.forEach { addVariable(it) }
        fireTableStructureChanged()
    }

    /**
     * Add a variable as a new data column if it does not already exist in the
     * table.
     *
     * @param variable: netCDF variable
     */
    fun addVariable(name: String) {
        val variable = file?.findVariable(name) ?: throw IllegalArgumentException("Invalid variable: $name")
        if (dimensions.isEmpty()) {
            dimensions = variable.publicDimensions
            dimensions.forEach { addCoordinateColumn(it) }
            val shape = dimensions.map { (0 until it.length) }.toTypedArray()
            index = if (shape.isEmpty()) emptyList() else cartProd(*shape).map { it.toIntArray() }.toList()
        } else if (!isCongruent(variable)) {
            throw IllegalArgumentException("Cannot add incongruent variable '${name}' to table")
        }
        if (columns.find { it.label == name } == null) {
            // Exclude duplicates, including variables that are already present
            // as coordinate variables.
            addVariableColumn(variable)
        }
    }

    /**
     * Clear all columns from the table.
     */
    fun resetData() {
        dimensions = emptyList()
        columns.clear()
        index = emptyList()
        file = null
    }

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
        val variable = file?.findVariable(dimension.fullNameEscaped)
        if (variable?.isCoordinateVariable != true) {
            // No coordinate variable for this dimension.
            val axis = dimensions.indexOf(dimension)
            columns.add(IndexColumn(dimension.fullNameEscaped, axis))
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

    /**
     * Returns the number of rows in the model. A
     * `JTable` uses this method to determine how many rows it
     * should display.  This method should be quick, as it
     * is called frequently during rendering.
     *
     * @return the number of rows in the model
     * @see .getColumnCount
     */
    override fun getRowCount() = index.size

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
     *
     */
    override fun getColumnClass(columnIndex: Int): Class<*> =
        columns[columnIndex].type

    /**
     * Get the name label for a column.
     */
    override fun getColumnName(columnIndex: Int) =
        columns[columnIndex].label

    /**
     * Returns the value for the cell at `columnIndex` and
     * `rowIndex`.
     *
     * @param   rowIndex        the row whose value is to be queried
     * @param   columnIndex     the column whose value is to be queried
     * @return  the value Object at the specified cell
     */
    override fun getValueAt(rowIndex: Int, columnIndex: Int) =
        columns[columnIndex].value(rowIndex)
}

