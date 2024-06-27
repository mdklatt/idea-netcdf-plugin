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
        val stringAttribute = model.getChild(model.root, 1)
        val numericAttribute = model.getChild(model.root, 13)
        assertEquals(":Conventions = \"CF-1.0\"", stringAttribute.toString())
        assertEquals(":realization = 1", numericAttribute.toString())
        val variables = model.getChild(model.root, 18)
        assertEquals("Variables", variables.toString())
        assertEquals(12, model.getChildCount(variables))
        assertEquals("area: float[128, 256]", model.getChild(variables, 0).toString())
    }
}
