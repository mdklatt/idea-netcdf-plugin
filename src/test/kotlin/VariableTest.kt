/**
 * Unit tests for Variable.kt
 */
package dev.mdklatt.idea.netcdf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import ucar.nc2.NetcdfFiles


/**
 * Test fixture for the Variable class extensions.
 */
internal class VariableTest {

    private val path = "src/test/resources/sresa1b_ncar_ccsm3-example.nc"
    private val file = NetcdfFiles.open(path)

    /**
     * Test the calendar extension property.
     */
    @Test
    fun testCalendar() {
        assertEquals("noleap", file.findVariable("time")?.calendar?.name)
        assertNull(file.findVariable("pr")?.calendar)
        return
    }

    /**
     * Test the typeString extension property.
     */
    @Test
    fun testTypeString() {
        assertEquals("float", file.findVariable("pr")?.typeString)
        assertEquals("time<double>", file.findVariable("time")?.typeString)
        return
    }

    /**
     * Test the dateUnits extension property.
     */
    @Test
    fun testDateUnits() {
        val time = file.findVariable("time")
        assertEquals("Day since 0000-01-01T00:00:00Z", time?.dateUnits?.toString())
        assertNull(file.findVariable("pr")?.dateUnits)
        return
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
        assertTrue(file.findVariable("time")?.isTime ?: false)
        assertFalse(file.findVariable("lat")?.isTime ?: false)
        return
    }

    /**
     * Test the publicDimensions extension property.
     */
    @Test
    fun testPublicDimensions() {
        // TODO: Test for array string variable.
        val variable = file.findVariable("pr")
        assertNotNull(variable)
        assertEquals(variable.dimensions, variable.publicDimensions)
        return
    }
}
