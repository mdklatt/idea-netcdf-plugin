/**
 * Unit tests for CdlFile.kt.
 */
package dev.mdklatt.idea.netcdf.files

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlin.io.path.createTempFile

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


/**
 * Unit tests for the WriteCdlFileAction class.
 */
internal class WriteCdlFileActionTest : BasePlatformTestCase() {

    private val ncPath = "src/test/resources/sresa1b_ncar_ccsm3-example.nc"

    /**
     * Test the writeSchema() method.
     */
    fun testWriteSchema() {
        // TODO: Compare output against 'sresa1b_ncar_ccsm3-example.cdl'.
        val cdlPath = createTempFile(suffix = ".cdl")
        val action = WriteCdlFileAction()
        action.writeSchema(ncPath, cdlPath.toString())
        val lines = cdlPath.toFile().readLines()
        assertEquals(0, lines[0].indexOf("netcdf"))
    }
}
