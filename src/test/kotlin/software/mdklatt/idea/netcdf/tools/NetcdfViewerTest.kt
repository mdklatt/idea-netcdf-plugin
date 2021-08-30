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
//internal class NetcdfToolWindowTest : BasePlatformTestCase() {  // JUnit3
//
//    private lateinit var window: NetcdfViewer
//
//    /**
//     * Per-test initialization.
//     */
//    override fun setUp() {
//        super.setUp()
//        window = NetcdfViewer()
//        return
//    }
//
//    /**
//     * Test the isApplicable attribute.
//     */
//    fun testIsApplicable() {
//        assertTrue(window.isApplicable(project))
//        return
//    }
//}


/**
 * Unit tests for the DataTableModel class.
 */
internal class DataTableModelTest {

    private val path = "src/test/resources/sresa1b_ncar_ccsm3-example.nc"
    private val file = NetcdfFile.open(path)
    private val model = DataTableModel()

    /**
     * Test the rowCount property.
     */
    @Test
    fun testRowCount() {
        assertEquals(0, model.rowCount)
        model.setData(file, sequenceOf("pr"))
        assertEquals(32768, model.rowCount)
    }

    /**
     * Test the columnCount property.
     */
    @Test
    fun testColumnCount() {
        assertEquals(0, model.columnCount)
        model.setData(file, sequenceOf("pr", "tas"))
        assertEquals(5, model.columnCount)
    }

    /**
     * Test the getColumnName() method.
     */
    @Test
    fun testGetColumnName() {
        model.setData(file, sequenceOf("pr", "tas"))
        assertEquals("time", model.getColumnName(0))
        assertEquals("tas", model.getColumnName(4))
    }

    /**
     * Test the getColumnClass() method.
     */
    @Test
    fun testGetColumnClass() {
        model.setData(file, sequenceOf("pr", "tas"))
        assertEquals(String::class.java, model.getColumnClass(0))
        assertEquals(Float::class.java, model.getColumnClass(4))
    }

    /**
     * Test the getValueAt() method.
     */
    @Test
    fun testGetCellValueAt() {
        // TODO: Add test for fixed-length strings.
        model.setData(file, sequenceOf("pr", "tas"))
        assertEquals("2000-05-16T12:00:00Z", model.getValueAt(0, 0))
        assertEquals(215.73935f, model.getValueAt(2, 4))
    }
}


/**
 * Unit tests for the SchemaTableModel class.
 */
internal class SchemaTableModelTest {

    private val path = "src/test/resources/sresa1b_ncar_ccsm3-example.nc"
    private val file = NetcdfFile.open(path)
    private val model = SchemaTableModel().apply { setData(file) }
    private val labels = arrayOf("Variable", "Description", "Dimensions", "Units", "Type")

    /**
     * Test the rowCount property.
     */
    @Test
    fun testRowCount() {
        assertEquals(12, model.rowCount)
    }

    /**
     * Test the columnCount property.
     */
    @Test
    fun testColumnCount() {
        assertEquals(labels.size, model.columnCount)
    }

    /**
     * Test the getColumnName() method.
     */
    @Test
    fun testGetColumnName() {
        labels.forEachIndexed { column, label ->
            assertEquals(label, model.getColumnName(column))
        }
    }

    /**
     * Test the getColumnClass() method.
     */
    @Test
    fun testGetColumnClass() {
        labels.indices.forEach {
            assertEquals(String::class.java, model.getColumnClass(it))
        }
    }

    /**
     * Test the getValueAt() method.
     */
    @Test
    fun testGetCellValueAt() {
        val row = listOf(
            "ua", "eastward_wind", "(time=1, plev=17, lat=128, lon=256)",
            "m s-1", "float"
        )
        row.forEachIndexed { column, value ->
            assertEquals(value, model.getValueAt(11, column))
        }
    }
}
