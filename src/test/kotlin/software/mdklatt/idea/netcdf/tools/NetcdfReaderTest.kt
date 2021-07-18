package software.mdklatt.idea.netcdf.tools

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 * Unit tests for the NetcdfReader2 class.
 */
internal class NetcdfReaderTest {

    private val path = "src/test/resources/sresa1b_ncar_ccsm3-example.nc"

    /**
     * Test the default constructor.
     */
    @Test
    fun testDefaultConstructor() {
        val reader = NetcdfReader()
        assertTrue(reader.isClosed)
    }

    /**
     * Test the open constructor.
     */
    @Test
    fun testOpenConstructor() {
        val reader = NetcdfReader(path)
        assertFalse(reader.isClosed)
        reader.close()
    }

    /**
     * Test the open() method.
     */
    @Test
    fun testOpen() {
        val reader = NetcdfReader()
        reader.open(path)
        assertFalse(reader.isClosed)
    }

    /**
     * Test the close() method.
     */
    @Test
    fun testClose() {
        val reader = NetcdfReader(path)
        reader.close()
        reader.close()  // should be safe if reader is already closed
        assertTrue(reader.isClosed)
    }

    /**
     * Test the use() method.
     */
    @Test
    fun testUse() {
        val reader = NetcdfReader(path)
        reader.use {
            assertFalse(reader.isClosed)
        }
        assertTrue(reader.isClosed)
    }

    /**
     * Test the rowCount property.
     */
    @Test
    fun testRowCount() {
        val reader = NetcdfReader(path)
        assertEquals(0, reader.rowCount)
        reader.setView(listOf("pr"))
        assertEquals(32768, reader.rowCount)
    }

    /**
     * Test the schema property.
     */
    @Test
    fun testSchema() {
        val reader = NetcdfReader()
        assertTrue(reader.schema.isEmpty())
        reader.open(path)
        assertEquals("air_temperature", reader.schema["tas"]!!.get("description"))
    }

    /**
     * Test the columns property.
     */
    @Test
    fun testColumns() {
        val columns = listOf("time", "lat", "lon", "pr", "tas")
        val reader = NetcdfReader(path)
        assertTrue(reader.columns.isEmpty())
        reader.setView(listOf("pr", "tas"))
        assertEquals(columns, reader.columns.toList())
    }

    /**
     * Test the rows() method.
     */
    @Test
    fun testRows() {
        val reader = NetcdfReader(path)
        assertTrue(reader.rows().toList().isEmpty())
        reader.setView(listOf("pr", "tas"))
        val rows = reader.rows(1, 3).toList()
        val time = rows.map { it[0] }.toList()
        assertTrue(time.all { it == "2000-05-16T12:00:00Z" })
        val tas = rows.map { it[4].toString().toFloat() }.toList()
        assertEquals(listOf(215.80531f, 215.73935f), tas)
    }
}
