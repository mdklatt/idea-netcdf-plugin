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
    private var variables = emptyMap<String, Variable>()
    private var coordinates = emptyMap<String, List<*>>()
    private var readShape = IntArray(0)
    private var indexes = emptyList<IntArray>()

    val isClosed
        get() = file == null

    val columns: Array<String>
        get() = (coordinates.keys + variables.keys).toTypedArray()

    val schema : Map<String, Map<String, String>>
        get() = file?.variables?.associate {
            Pair(it.fullName, mapOf(
                    "description" to it.description,
                    "dimensions" to it.nameAndDimensions.substring(it.nameAndDimensions.lastIndexOf("(")),
                    "units" to it.unitsString,
                    "type" to it.dataType.name.toLowerCase(),
            ))
        } ?: emptyMap()

    val rowCount
        get() = indexes.size

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
        variables = emptyMap()
        coordinates = emptyMap()
    }

    /**
     * Set the active view.
     *
     * The view controls which variables are read from the netCDF dataset. All
     * selected variables must have congruent dimensions. Variables that will
     * be displayed as coordinates will be ignored.
     *
     * @param varNames: variables to read from
     */
    fun setView(varNames: Iterable<String>) {
        variables = varNames.toSet().associate {
            val variable = file!!.findVariable(it) ?: throw IllegalArgumentException("unknown variable '${it}'")
            Pair(variable.fullName, variable)
        }
        val dimensions = variables.values.firstOrNull()?.dimensions ?: emptyList()
        coordinates = dimensions.associate { Pair(it.fullName, coordValues(it)) }
        val dimsShape = dimensions.map { it.length }.toIntArray()
        readShape = IntArray(coordinates.size) { 1 }
        val axes = dimsShape.map { (0 until it) }.toTypedArray()
        indexes = if (axes.isEmpty()) emptyList() else cartProd(*axes).map { it.toIntArray() }.toList()
    }

    /**
     * Read all active variables at a single index.
     *
     * @return: array of coordinates and variable values
     */
    private fun read(index: IntArray): Array<Any?> {
        val coordValues = coordinates.values.mapIndexed {
            pos, value -> value[index[pos]]
        }
        val dataValues = variables.values.map {
            it.read(index, readShape)
        }
        return (coordValues + dataValues).toTypedArray()
    }

    /**
     * Get rows from the current view using a flattened index.
     *
     * @param start: starting index
     * @param end: ending index (exclusive)
     * @return: sequence of select rows
     */
    fun rows(start: Int, end: Int) : Sequence<Array<Any?>> {
        return (start until end).map {
            read(indexes[it])
        }.asSequence()
    }

    /**
     * @overload
     */
    fun rows(start: Int = 0) : Sequence<Array<Any?>> {
        return rows(start, indexes.size)
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
