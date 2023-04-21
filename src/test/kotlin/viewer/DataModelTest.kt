/**
 * Unit tests for DataModel.kt
 */
package dev.mdklatt.idea.netcdf.viewer

import org.junit.jupiter.api.AfterEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ucar.nc2.NetcdfFiles


/**
 * Unit tests for the DataModel class.
 */
internal class DataModelTest {

    private val path = "src/test/resources/sresa1b_ncar_ccsm3-example.nc"
    private val file = NetcdfFiles.open(path)
    private val model = DataModel().apply {
        pageSize = 10
        fillTable(file, sequenceOf("pr", "tas"))
    }
    private val labels = listOf("time", "lat", "lon", "pr", "tas")

    /**
     * Per-test cleanup.
     */
    @AfterEach
    fun tearDown() {
        model.clearTable()
        file.close()
    }

    /**
     * Test the labels property.
     */
    @Test
    fun testLabels() {
        model.fillTable(file, sequenceOf("pr", "tas"))
        assertEquals(labels, model.labels)
    }

    /**
     * Test the clear() method.
     */
    @Test
    fun testClearTable() {
        model.clearTable()
        assertTrue(model.labels.isEmpty())
        assertEquals(0, model.rowCount)
        assertEquals(0, model.columnCount)
    }

    /**
     * Test the rowCount property.
     */
    @Test
    fun testRowCount() {
        model.fillTable(file, sequenceOf("pr"))
        assertEquals(10, model.rowCount)
        model.pageNumber = 3277
        assertEquals(8, model.rowCount)
    }

    /**
     * Test the columnCount property.
     */
    @Test
    fun testColumnCount() {
        assertEquals(5, model.columnCount)
    }

    /**
     * Test the getColumnName() method.
     */
    @Test
    fun testGetColumnName() {
        assertEquals("time", model.getColumnName(0))
        assertEquals("tas", model.getColumnName(4))
    }

    /**
     * Test the getColumnClass() method.
     */
    @Test
    fun testGetColumnClass() {
        assertEquals(String::class.java, model.getColumnClass(0))
        assertEquals(Float::class.java, model.getColumnClass(4))
    }

    /**
     * Test the getValueAt() method.
     */
    @Test
    fun testGetCellValueAt() {
        // TODO: Add test for fixed-length strings.
        assertEquals("2000-05-16T12:00:00Z", model.getValueAt(0, 0))
        model.pageNumber = 2
        assertEquals(215.08086f, model.getValueAt(1, 4))
    }
}
