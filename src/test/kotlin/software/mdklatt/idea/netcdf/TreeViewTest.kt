/**
 * Unit tests for the NetcdfViewer module.
 */
package software.mdklatt.idea.netcdf.software.mdklatt.idea.netcdf

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import software.mdklatt.idea.netcdf.TreeView
import ucar.nc2.NetcdfFile
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * Unit tests for the TreeView class.
 */
internal class TreeViewTest {

    private val path = "src/test/resources/sresa1b_ncar_ccsm3-example.nc"
    private val file = NetcdfFile.open(path)
    private val view = TreeView(file)

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
        val labels = view.root.variables.map { it.label }
        assertEquals(12, labels.count())
        assertEquals("area (float)", labels.first())
        assertEquals("ua (float)", labels.last())
        return
    }
}
