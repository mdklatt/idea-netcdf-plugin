/**
 * Extensions for the ucar.nc2.Variable class
 */
package dev.mdklatt.idea.netcdf

import ucar.nc2.Dimension
import ucar.nc2.Variable
import ucar.nc2.time.Calendar
import ucar.nc2.time.CalendarDateUnit


/**
 * Calendar object if this is a time variable, or else null.
 */
internal val Variable.calendar : Calendar?
    get() = if (!isTime) null else Calendar.get(findAttribute("calendar")?.stringValue) ?: Calendar.getDefault()


/**
 * Data type description.
 */
internal val Variable.typeString : String
    get() {
        val typeString = if (isArrayString) "char[${shape.last()}]" else dataType.name.lowercase()
        return if (isTime) "time<${typeString}>" else typeString
    }


/**
 * Date units object if this variable has a calendar, or else null.
 */
internal val Variable.dateUnits : CalendarDateUnit?
    get() = calendar?.let { CalendarDateUnit.of(it.name, unitsString) }


/**
 * True if variable appears to be a time variable. The variable is assumed to
 * contain time values if it adheres to netCDF time conventions, namely that it
 * is a numeric variable whose name starts with 'time' and has a 'units'
 * attribute of the form '<units> since <timestamp>'
 *
 * @see <a href="https://www.unidata.ucar.edu/software/netcdf/time/recs.html">A Brief History of (netCDF) Time<a>
 */
internal val Variable.isTime : Boolean
    get() {
        val name = fullNameEscaped.split("/").last()
        val regex = CalendarDateUnit.udunitPatternString.toRegex()
        val isTimeUnits = unitsString?.let { regex.matches(it.lowercase()) }
        return name.startsWith("time", 0) && dataType.isNumeric && isTimeUnits == true
    }


/**
 * True if variable appears to be a character array string. Prior to netCDF4,
 * strings had to be stored as a 2D CHAR array where the second dimension
 * extends along the length of each string.
 *
 * @see <a href=http://www.bic.mni.mcgill.ca/users/sean/Docs/netcdf/guide.txn_58.html>Reading and Writing Character String Values</a>
 */
internal val Variable.isArrayString: Boolean
    get() = dataType.name.lowercase() == "char" && shape.size == 2


/**
 * Base variable name without group prefixes.
 */
internal val Variable.nameEscaped: String
    get() = fullNameEscaped.split("/").last()


/**
 * Variable dimensions excluding "private" dimensions, e.g. the length
 * dimension of character array string.
 */
internal val Variable.publicDimensions : List<Dimension>
    get() {
        return if (isArrayString) dimensions.dropLast(1) else dimensions
    }
