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
class NetcdfReader() : AutoCloseable {

    private companion object {
        internal fun isTime(variable: Variable) : Boolean {
            val name = variable.fullName.split("/").last()
            return name.startsWith("time", 0) && variable.dataType.isNumeric
        }
    }

    private val logger = Logger.getInstance(this::class.java)  // runtime class resolution
    private var file: NetcdfFile? = null
    private var _variables = emptyMap<String, Variable>()

    public val isClosed
        get() = file == null

    public val variables
        get() = _variables

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
        _variables = file!!.variables.associateBy { it.fullName }
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
        _variables = emptyMap()
        return
    }

    /**
     * Read variable data.
     *
     * All requested variables should have the same dimensions.
     *
     * @return
     */
    public fun read(varNames: Iterable<String>): Sequence<Map<String, Any>> {
        // TODO: Handle _FillValue.
        val variables = _variables.filterKeys { varNames.contains(it) }.values
        val dimensions = variables.first().dimensions
        val axesCoords = dimensions.map { (0 until it.length) }
        val axesValues = dimensions.map { dimensionValues(it).map {value -> value.toString() }.toList() }
        val expandedCoords = cartProd(*axesCoords.toTypedArray())
        val expandedValues = cartProd(*axesValues.toList().toTypedArray())
        val dimNames = dimensions.map { it.fullName }
        val shape = IntArray(dimensions.size) { 1 }
        return expandedCoords.zip(expandedValues).map {
            val coords = it.first.toIntArray()
            val values = variables.map { it.read(coords, shape) }
            dimNames.zip(it.second).toMap() + varNames.zip(values).toMap()
        }.asSequence()
    }

    /**
     * Get dimension variable values.
     *
     * If there is no matching dimension variable, the values are the dimension
     * indexes.
     *
     * @param dimension: variable dimension
     * @return dimension values
     */
    private fun dimensionValues(dimension: Dimension): Sequence<*> {
        val variable = file?.findVariable(dimension.fullName)
        if (variable?.isCoordinateVariable != true) {
            return (0 until dimension.length).asSequence()
        }
        val values = mutableListOf<Any>()
        variable.read().indexIterator.let {
            // TODO: Turn this into a Sequence without intermediate list.
            while (it.hasNext()) {
                values.add(it.objectNext)
            }
        }
        if (isTime(variable)) {
            // Convert numeric times to ISO 8601 strings.
            // TODO: Convert non-dimension variables as well.
            val calendar = variable.findAttribute("calendar")?.stringValue ?: Calendar.getDefault().name
            val timeUnits = CalendarDateUnit.of(calendar, variable.unitsString)
            values.replaceAll {
                val offset = it.toString().toDouble()
                timeUnits.makeCalendarDate(offset).toString()
            }
        }
        return values.asSequence()
    }
}
