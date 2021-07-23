/**
 * Unit tests for the NetcdfViewer module.
 */
package software.mdklatt.idea.netcdf.tools

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.jupiter.api.Test
import ucar.ma2.DataType
import ucar.nc2.NetcdfFile
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
        model.setData(file, sequenceOf("pr", "tas"))
        assertEquals("2000-05-16T12:00:00Z", model.getValueAt(0, 0))
        assertEquals(215.73935f, model.getValueAt(2, 4))
    }
}
