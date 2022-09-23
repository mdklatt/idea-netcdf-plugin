/**
 * Unit tests for the NetcdFile module.
 */
package dev.mdklatt.idea.netcdf.files

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


/**
 * Unit tests for the NetdfFileType class.
 */
internal class NetcdfFileTypeTest {

    private var type = NetcdfFileType()

    /**
     * Test the name attribute.
     */
    @Test
    fun testName() {
        assertEquals("netCDF", type.name)
    }

    /**
     * Test the defaultExtension property.
     */
    @Test
    fun testDefaultExtenstion() {
        assertEquals("nc", type.defaultExtension)
    }

    /**
     * Test the icon property.
     */
    @Test
    fun testIcon() {
        assertNotNull(type.icon)
    }

    /**
     * Test the description property.
     */
    @Test
    fun testDescription() {
        assertTrue(type.description.isNotEmpty())
    }

    /**
     * Test the isBinary property.
     */
    @Test
    fun testIsBinary() {
        assertTrue(type.isBinary)
    }
}
