/**
 * Unit tests for the NetcdfViewer module.
 */
package software.mdklatt.idea.netcdf.tools

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.jupiter.api.Test
import ucar.nc2.NetcdfFile
import kotlin.test.assertEquals

// The IDEA platform tests use JUnit3, so method names are used to determine
// behavior instead of annotations. Notably, test classes are *not* constructed
// before each test, so setUp() methods should be used for initialization.
// Also, test functions must be named `testXXX` or they will not be found
// during automatic discovery.

/**
 * Unit tests for the NetcdfToolWindow class.
 */
internal class NetcdfToolWindowTest : BasePlatformTestCase() {  // JUnit3

    private lateinit var window: NetcdfToolWindow

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        window = NetcdfToolWindow()
        return
    }

    /**
     * Test the isApplicable attribute.
     */
    fun testIsApplicable() {
        assertTrue(window.isApplicable(project))
        return
    }
}


/**
 *
 */
//internal class NetcdfTableModelTest {  // JUnit5
//
//    val reader = NetcdfReader("src/test/resources/sresa1b_ncar_ccsm3-example.nc")
//    val model = NetcdfTableModel()
//
//    /**
//     * Test the readSchema() method.
//     */
//    @Test
//    internal fun testReadSchema() {
//        model.readSchema(reader)
//        assertEquals(listOf("Variable", "Description", "Units", "Type"), labels())
//    }
//
//    /**
//     * Test the readSchema() method.
//     */
//    @Test
//    internal fun testReadData() {
//        fun column(name: String) : List<String> {
//            val colIndex = labels().indexOf(name)
//            return (1 until model.rowCount).map {
//                model.getValueAt(it, colIndex).toString()
//            }.toList()
//        }
//        model.readData(reader, listOf("pr", "tas"))
//        assertEquals(listOf("(time, lat, lon)", "pr", "tas"), labels())
//        val labels = listOf("(730135.5, -88.927734, 1.40625)", "(730135.5, -88.927734, 2.8125)")
//        assertEquals(labels, column("(time, lat, lon)").subList(0, 2))
//        val values = listOf("215.80531 ", "215.73935 ")
//        assertEquals(values, column("tas").subList(0, 2))
//    }
//
//    /**
//     * Get column labels.
//     */
//    private fun labels() = (0 until model.columnCount).map { model.getColumnName(it)}.toList()
//}
