package software.mdklatt.idea.netcdf.tools

import com.intellij.openapi.diagnostic.Logger
import ucar.nc2.Dimension
import ucar.nc2.NetcdfFile
import ucar.nc2.Variable
import ucar.nc2.time.Calendar
import ucar.nc2.time.CalendarDateUnit
import vendor.tandrial.itertools.cartProd


/**
 * Read a netCDF file.
 */
internal class NetcdfReader() : AutoCloseable {

    private val logger = Logger.getInstance(this::class.java)  // runtime class resolution
    private var file: NetcdfFile? = null
    private var variables = emptyList<Variable>()
    private var dimensions = emptyList<Dimension>()
    private var coordinates = emptyMap<String, List<*>>()
    private var dimsShape = IntArray(0)
    private var readShape = IntArray(0)
    private var _columns = emptyArray<String>()
    private var _indexes = emptyList<IntArray>()

    val isClosed
        get() = file == null

    val columns: Array<String>
        get() {
            val dimNames = dimensions.map { it.fullName }.toList()
            val varNames = variables.map { it.fullName }.toList()
            return (dimNames + varNames).toTypedArray()
        }

    val indexes: Sequence<IntArray>
        get() = _indexes.asSequence()

    val schema : Map<String, Map<String, String>>
        get() = file?.variables?.map {
            Pair(it.fullName, mapOf(
                "description" to it.description,
                "dimensions" to it.nameAndDimensions.substring(it.nameAndDimensions.lastIndexOf("(")),
                "units" to it.unitsString,
                "type" to it.dataType.name.toLowerCase(),
            ))
        }?.toMap() ?: emptyMap()

    val rowCount
        get() = _indexes.size

    /**
     * Construct an instance from a netCDF file path.
     */
    constructor(path: String) : this() {
        open(path)
    }

    /**
     * Open a netCDF reader.
     *
     * @param path: netCDF file path
     */
    fun open(path: String) {
        if (path != file?.location) {
            file?.close()
            logger.info("Opening netCDF file $path")
            file = NetcdfFile.open(path)
        }
        return
    }

    /**
     * Closes this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the
     * `try`-with-resources statement.
     */
    override fun close() {
        file?.close()
        file = null
        variables = emptyList()
    }

    /**
     * Set the active cursor.
     *
     * The cursor controls which variables are read from the netCDF dataset.
     * All selected variables must have congruent dimensions.
     *
     * @param varNames: variables to read from
     */
    fun setCursor(varNames: Iterable<String>) {
        variables = varNames.toSet().map {
            file!!.findVariable(it) ?: throw IllegalArgumentException("unknown variable '${it}'")
        }
        dimensions = variables.firstOrNull()?.dimensions ?: emptyList()
        coordinates = dimensions.map { Pair(it.fullName, coordValues(it)) }.toMap()
        dimsShape = dimensions.map { it.length }.toIntArray()
        readShape = IntArray(coordinates.size) { 1 }
        val axes = dimsShape.map { (0 until it) }.toTypedArray()
        _indexes = cartProd(*axes).map { it.toIntArray() }.toList()
    }

    /**
     * Read all cursor variables at a single index.
     *
     * @return: array of coordinates and variable values
     */
    fun read(index: IntArray): Array<Any?> {
        val record = coordinates.values.mapIndexed {
            pos, value -> value[index[pos]]
        }.toMutableList()
        variables.forEach {
            record.add(it.read(index, readShape))
        }
        return record.toTypedArray()
    }

    /**
     * Get rows from the current cursor using a flattened index.
     *
     * @param start: starting index
     * @param end: ending index (exclusive)
     * @return: sequence of select rows
     */
    fun rows(start: Int, end: Int) : Sequence<Array<Any?>> {
        return (start until end).map {
            read(_indexes[it])
        }.asSequence()
    }

    /**
     * @overload
     */
    fun rows(start: Int = 0) : Sequence<Array<Any?>> {
        return rows(start, _indexes.size)
    }

    /**
     * Flattened index values for the active cursor.
     */
    fun indexes() : Sequence<IntArray> {
        val axes = dimsShape.map { (0 until it) }.toTypedArray()
        return cartProd(*axes).map { it.toIntArray() }.asSequence()
    }

    /**
     * Get the coordinate values for a dimension.
     *
     * The values of the dimension's coordinate variable are used if it exists.
     * Otherwise, the coordinate values are simply the index positions along
     * the dimension, i.e. [0, 1,...n).
     *
     * @param dimension: dimension to use
     * @return: coordinate values
     */
    private fun coordValues(dimension: Dimension) : List<*> {
        val variable = file!!.findVariable(dimension.fullName)
        if (variable?.isCoordinateVariable != true) {
            return (0 until dimension.length).toList()
        }
        else if (isTime(variable)) {
            return timeValues(variable).toList()
        }
        val iter = variable.read().indexIterator
        return generateSequence {
            if (iter.hasNext()) iter.objectNext else null
        }.toList()
    }

    /**
     * Convert time variable values to ISO 8601 strings.
     *
     * @param variable: time variable
     * @return: sequence of time strings
     */
    private fun timeValues(variable: Variable) : Sequence<String> {
        val calendar = variable.findAttribute("calendar")?.stringValue ?: Calendar.getDefault().name
        val units = CalendarDateUnit.of(calendar, variable.unitsString)
        fun timeValue(value: Any) : String {
            return units.makeCalendarDate(value.toString().toDouble()).toString()
        }
        val iter = variable.read().indexIterator
        return generateSequence {
            if (iter.hasNext()) timeValue(iter.objectNext) else null
        }
    }

    /**
     * Test if a variable appears to be a time variable.
     *
     * @param variable: variable to test
     * @return: true if this appears to be a time variable
     */
    private fun isTime(variable: Variable) : Boolean {
        // TODO: Test units attribute.
        val name = variable.fullName.split("/").last()
        return name.startsWith("time", 0) && variable.dataType.isNumeric
    }
}
