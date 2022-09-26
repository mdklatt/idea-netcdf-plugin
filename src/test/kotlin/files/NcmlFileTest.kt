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


/**
 * Unit tests for the WriteCdlFileAction class.
 */
internal class WriteNcmlFileActionTest : BasePlatformTestCase() {

    private val ncPath = "src/test/resources/sresa1b_ncar_ccsm3-example.nc"

    /**
     * Test the writeSchema() method.
     */
    fun testWriteSchema() {
        // TODO: Compare output against 'sresa1b_ncar_ccsm3-example.cdl'.
        val ncmlPath = kotlin.io.path.createTempFile(suffix = ".ncml")
        val action = WriteNcmlFileAction()
        action.writeSchema(ncPath, ncmlPath.toString())
//        val lines = cdlPath.toFile().readLines()
//        assertEquals(0, lines[0].indexOf("netcdf"))
    }
}
