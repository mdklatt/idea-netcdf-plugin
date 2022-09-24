/**
 * Unit tests for the NetcdfViewer module.
 */
package dev.mdklatt.idea.netcdf

import kotlin.test.Test
import kotlin.test.assertEquals
import ucar.nc2.NetcdfFile


/**
 * Unit tests for the FileView class.
 */
internal class FileViewTest {

    private val path = "src/test/resources/sresa1b_ncar_ccsm3-example.nc"
    private val file = NetcdfFile.open(path)
    private val view = FileView(file)

    /**
     * Test the groups property.
     */
    @Test
    fun testGroups() {
        // TODO: Need a test file with groups.
        assertEquals(0, view.root.groups.count())
        return
    }

    /**
     * Test the variables property.
     */
    @Test
    fun testVariables() {
        val labels = view.root.variables.map { it.toString() }
        assertEquals(12, labels.count())
        assertEquals("area: float[128, 256]", labels.first())
        assertEquals("ua: float[1, 17, 128, 256]", labels.last())
        return
    }
}
