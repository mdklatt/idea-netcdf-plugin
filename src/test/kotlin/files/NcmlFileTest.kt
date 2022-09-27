/**
 * Unit tests for the NcmlFile module.
 */
package dev.mdklatt.idea.netcdf.files

import com.intellij.testFramework.fixtures.BasePlatformTestCase


// The IDEA platform tests use JUnit3, so method names are used to determine
// behavior instead of annotations. Notably, test classes are *not* constructed
// before each test, so setUp() methods should be used for initialization.
// Also, test functions must be named `testXXX` or they will not be found
// during automatic discovery.



/**
 * Unit tests for the NcmlFileType class.
 */
internal class NcmlFileTypeTest : BasePlatformTestCase() {

    private lateinit var type: NcmlFileType

    /**
     * Per-test setup
     */
    override fun setUp() {
        super.setUp()
        type = NcmlFileType()
    }

    /**
     * Test the defaultExtension property.
     */
    fun testDefaultExtenstion() {
        assertEquals("ncml", type.defaultExtension)
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
