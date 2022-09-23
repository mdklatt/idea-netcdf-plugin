/**
 * Unit tests for the NcmlFile module.
 */
package dev.mdklatt.idea.netcdf.files

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for the NcmlFileType class.
 */
internal class NcmlFileTypeTest {

    private var type = NcmlFileType()

    /**
     * Test the defaultExtension property.
     */
    @Test
    fun testDefaultExtenstion() {
        assertEquals("ncml", type.defaultExtension)
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
        assertFalse(type.isBinary)
    }
}
