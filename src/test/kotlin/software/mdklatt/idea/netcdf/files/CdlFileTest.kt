/**
 * Unit tests for the CdlFile module.
 */
package software.mdklatt.idea.netcdf.files

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for the CdlFileType class.
 */
internal class CdlFileTypeTest {

    private var type = CdlFileType()

    /**
     * Test the defaultExtension property.
     */
    @Test
    fun testDefaultExtenstion() {
        assertEquals("cdl", type.defaultExtension)
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
