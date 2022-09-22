/**
 * Unit tests for the NetcdfViewer module.
 */
package software.mdklatt.idea.netcdf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import ucar.nc2.NetcdfFile


/**
 * Unit tests for the DataView class.
 */
internal class DataViewTest {

    private val path = "src/test/resources/sresa1b_ncar_ccsm3-example.nc"
    private val file = NetcdfFile.open(path)
    private val view = DataView(file).also { it.add("pr", "tas", "tas") }  // test duplicate add
    private val labels = listOf("time", "lat", "lon", "pr", "tas")

    /**
     * Test the labels property.
     */
    @Test
    fun testLabels() {
        assertEquals(labels, view.labels)
        return
    }

    /**
     * Test the rowCount property.
     */
    @Test
    fun testRowCount() {
        assertEquals(32768, view.rowCount)
    }

    /**
     * Test the columnCount property.
     */
    @Test
    fun testColumnCount() {
        assertEquals(5, view.columnCount)
    }

    /**
     * Test the column() method for an index.
     */
    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3, 4])
    fun testColumnIndex(index: Int) {
        assertEquals(labels[index], view.column(index).label)
        return
    }

    /**
     * Test the column() method for a label.
     */
    @ParameterizedTest
    @ValueSource(strings = ["time", "lat", "lon", "pr", "tas"])
    fun testColumnLabel(label: String) {
        assertEquals(label, view.column(label).label)
        return
    }

    /**
     * Test the Column.value() method.
     */
    @Test
    fun testValue() {
        assertEquals("2000-05-16T12:00:00Z", view.column(0).value(0))
        assertEquals(215.73935f, view.column(4).value(2))
        return
    }

    /**
     * Test the clear() method.
     */
    @Test
    fun testClear() {
        view.clear()
        assertTrue(view.labels.isEmpty())
        return
    }
}
