/**
 * Unit tests for the FileModel.kt;
 */
package dev.mdklatt.idea.netcdf.viewer

import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.AfterEach
import ucar.nc2.NetcdfFiles


/**
 * Unit tests for the FileModel class.
 */
internal class FileModelTest {

    private val path = "src/test/resources/sresa1b_ncar_ccsm3-example.nc"
    private val file = NetcdfFiles.open(path)
    private val model = FileModel()

    /**
     * Per-test cleanup.
     */
    @AfterEach
    fun tearDown() {
        model.clearTree()
        file.close()
    }

    /**
     * Test the fillTree() method.
     */
    @Test
    fun testFillTree() {
        model.fillTree(file)
        assertEquals(file.location, model.root.toString())
        val variablesNode = model.getChild(model.root, 18)
        assertEquals("Variables", variablesNode.toString())
        assertEquals(12, model.getChildCount(variablesNode))
        assertEquals("area: float[128, 256]", model.getChild(variablesNode, 0).toString())
    }
}
