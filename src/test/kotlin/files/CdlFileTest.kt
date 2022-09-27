/**
 * Unit tests for CdlFile.kt.
 */
package dev.mdklatt.idea.netcdf.files

import com.intellij.testFramework.fixtures.BasePlatformTestCase


/**
 * Unit tests for the CdlFileType class.
 */
internal class CdlFileTypeTest : BasePlatformTestCase() {

    private lateinit var type: CdlFileType

    /**
     * Per-test setup
     */
    override fun setUp() {
        super.setUp()
        type = CdlFileType()
    }

    /**
     * Test the defaultExtension property.
     */
    fun testDefaultExtenstion() {
        assertEquals("cdl", type.defaultExtension)
    }

    /**
     * Test the icon property.
     */
    fun testIcon() {
        assertNotNull(type.icon)
    }

    /**
     * Test the description property.
     */
    fun testDescription() {
        assertTrue(type.description.isNotEmpty())
    }

    /**
     * Test the isBinary property.
     */
    fun testIsBinary() {
        assertFalse(type.isBinary)
    }
}
