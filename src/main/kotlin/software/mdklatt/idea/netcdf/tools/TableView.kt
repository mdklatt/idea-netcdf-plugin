package software.mdklatt.idea.netcdf.tools

import ucar.nc2.Dimension
import ucar.nc2.NetcdfFile
import ucar.nc2.Variable
import ucar.nc2.time.Calendar
import ucar.nc2.time.CalendarDateUnit
import vendor.tandrial.itertools.cartProd
import java.lang.IllegalStateException

/**
 * Represent n-dimensional netCDF variables as a two-dimensional table where
 * variables are mapped to columns in flattened row-major order.
*/
internal class TableView(private var file: NetcdfFile) {
    /**
     * Define a table column.
     */
    abstract inner class Column(val label: String) {
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
        private val axes: IntArray = variable.dimensions.map {
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

        private val calendar = variable.findAttribute("calendar")?.stringValue ?: Calendar.getDefault().name
        private val units = CalendarDateUnit.of(calendar, variable.unitsString)

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

    private var dimensions = emptyList<Dimension>()
    private var index : List<IntArray> = emptyList()
    private var columns = mutableListOf<Column>()

    val labels : List<String>
        get() = columns.map { it.label }.toList()

    val rowCount : Int
        get() = index.size

    val columnCount : Int
        get() = labels.size

    /**
     * Add a variable as a new data column if it does not already exist in the
     * table.
     *
     * @param name: fully-escaped variable name
     */
    fun add(name: String) {
        if (columns.find { it.label == name } != null) {
            return  // column already exists
        }
        val variable = file.findVariable(name) ?: throw IllegalArgumentException("unknown variable: $name")
        if (dimensions.isEmpty()) {
            dimensions = variable.dimensions
            dimensions.forEach { addCoordinateColumn(it) }
            val shape = dimensions.map { (0 until it.length) }.toTypedArray()
            index = if (shape.isEmpty()) emptyList() else cartProd(*shape).map { it.toIntArray() }.toList()
        } else if (!congruent(variable)) {
            throw IllegalArgumentException("cannot add incongruent variable $name to table")
        }
        addVariableColumn(variable)
        return
    }

    /** @overload */
    fun add(names: Sequence<String>) {
        names.forEach { add(it) }
        return
    }

    /** @overload */
    fun add(vararg names: String) {
        add(names.asSequence())
        return
    }

    /**
     * Clear all columns from the table.
     */
    fun clear() {
        dimensions = emptyList()
        columns.clear()
        index = emptyList()
    }

    /**
     * Get a column by index.
     *
     * @param index: column index
     * @return: selected column
     */
    fun column(index: Int) = columns[index]

    /**
     * Find a column by label. An IllegalArgumentException is thrown if the
     * label cannot be found.
     *
     * @param label: column label
     * @return: selected column
     */
    fun column(label: String) = columns.find { it.label == label } ?: throw IllegalArgumentException("unknown column: $label")

    /**
     * Test if a variable has the same dimensions as the current table
     * dimensions, irrespective of order.
     *
     * @return: true if the variable is compatible
     */
    private fun congruent(variable: Variable) : Boolean {
        // TODO: Fix for "classic" strings which have extra length dimension
        return setOf(dimensions) == setOf(variable.dimensions)
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
        val variable = file.findVariable(dimension.fullNameEscaped)
        if (variable?.isCoordinateVariable != true) {
            // No coordinate variable for this dimension.
            val axis = dimensions.indexOf(dimension)
            val column = object : Column(dimension.fullNameEscaped) {
                // Numeric index.
                override val type = Long::class.java
                override fun value(row: Int) = index[row][axis]
            }
            columns.add(column)
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
 * True if variable appears to be a character array string. Prior to netCDF4,
 * strings had to be stored as a 2D CHAR array where the second dimension
 * extends along the length of each string.
 *
 * @see <a href=http://www.bic.mni.mcgill.ca/users/sean/Docs/netcdf/guide.txn_58.html>Reading and Writing Character String Values</a>
 */
private val Variable.isArrayString: Boolean
    get() = dataType.name.toLowerCase() == "char" && shape.size == 2


/**
 * True if variable appears to be a time variable. The variable is assumed to
 * contain time values if it adheres to netCDF time conventions, namely that it
 * is a numeric variable whose name starts with 'time' and has a 'units'
 * attribute of the form '<units> since <timestamp>'
 *
 * @see <a href="https://www.unidata.ucar.edu/software/netcdf/time/recs.html">A Brief History of (netCDF) Time<a>
 */
private val Variable.isTime : Boolean
    get() {
        val name = fullNameEscaped.split("/").last()
        val regex = CalendarDateUnit.udunitPatternString.toRegex()
        return name.startsWith("time", 0) && dataType.isNumeric && regex.matches(unitsString.toLowerCase())
    }
