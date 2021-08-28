/**
 * Unit tests for Variable.kt
 */
package software.mdklatt.idea.netcdf

import org.junit.jupiter.api.Test
import ucar.nc2.NetcdfFile
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull


/**
 * Test fixture for the Variable class extensions.
 */
internal class VariableTest {

    private val path = "src/test/resources/sresa1b_ncar_ccsm3-example.nc"
    private val file = NetcdfFile.open(path)

    /**
     * Test the calendar extension property.
     */
    @Test
    fun testCalendar() {
        val time = file.findVariable("time")
        assertEquals("noleap", time.calendar?.name)
        assertNull(file.findVariable("pr").calendar)
    }

    /**
     * Test the dateUnits extension property.
     */
    @Test
    fun testDateUnits() {
        val time = file.findVariable("time")
        assertEquals("Day since 0000-01-01T00:00:00Z", time.dateUnits?.toString())
        assertNull(file.findVariable("pr").dateUnits)
    }

    /**
     * Test the isArrayString extension property.
     */
    @Test
    fun testIsArrayString() {
        // TODO: Need char[] variable in test file.
    }

    /**
     * Test the isTime extension property.
     */
    @Test
    fun testIsTime() {
        assertTrue(file.findVariable("time").isTime)
        assertFalse(file.findVariable("lat").isTime)
        return
    }

    /**
     * Test the nameEscaped extension property.
     */
    @Test
    fun testNameEscaped() {
        val variable = file.findVariable("pr")
        assertEquals("pr", variable.nameEscaped)
        return
    }

    /**
     * Test the publicDimensions extension property.
     */
    @Test
    fun testPublicDimensions() {
        // TODO: Test for array string variable.
        val variable = file.findVariable("pr")
        assertEquals(variable.dimensions, variable.publicDimensions)
        return
    }
}
