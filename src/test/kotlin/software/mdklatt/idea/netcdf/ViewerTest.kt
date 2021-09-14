/**
 * Unit tests for the Viewer module.
 */
package software.mdklatt.idea.netcdf.software.mdklatt.idea.netcdf

import org.junit.jupiter.api.Test
import software.mdklatt.idea.netcdf.DataTab
import software.mdklatt.idea.netcdf.FileTab
import ucar.nc2.NetcdfFile
import kotlin.test.assertEquals


/**
 * Unit tests for the DataTab.Model class.
 */
internal class DataTabModelTest {

    private val path = "src/test/resources/sresa1b_ncar_ccsm3-example.nc"
    private val file = NetcdfFile.open(path)
    private val model = DataTab.Model()

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
 * Unit tests for the FileTab.Model class.
 */
internal class FileTabModelTest {

    private val path = "src/test/resources/sresa1b_ncar_ccsm3-example.nc"
    private val file = NetcdfFile.open(path)
    private val model = FileTab.Model().apply { setData(file) }
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
