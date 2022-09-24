/**
 * Unit tests for the NetcdfViewer module.
 */
package dev.mdklatt.idea.netcdf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ucar.nc2.NetcdfFile


/**
 * Unit tests for the DataView class.
 */
internal class DataModelTest {

    private val path = "src/test/resources/sresa1b_ncar_ccsm3-example.nc"
    private val file = NetcdfFile.open(path)
    private val model = DataModel()
    private val labels = listOf("time", "lat", "lon", "pr", "tas")

    /**
     * Test the labels property.
     */
    @Test
    fun testLabels() {
        model.setData(file, sequenceOf("pr", "tas"))
        assertEquals(labels, model.labels)
        return
    }

    /**
     * Test the clear() method.
     */
    @Test
    fun testResetData() {
        model.resetData()
        assertTrue(model.labels.isEmpty())
        return
    }

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
