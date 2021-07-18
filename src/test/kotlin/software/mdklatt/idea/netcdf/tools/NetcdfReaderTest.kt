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
     * Test the indexes() method.
     */
    @Test
    fun testIndexes() {
        val reader = NetcdfReader(path)
        reader.setCursor(listOf("pr", "tas"))
        val indexes = reader.indexes().toList()
        assertEquals(listOf(0, 0, 0), indexes.first().toList())
        assertEquals(listOf(0, 127, 255), indexes.last().toList())
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
        reader.setCursor(listOf("pr", "tas"))
        assertEquals(columns, reader.columns.toList())
    }

    /**
     * Test the read() method.
     */
    @Test
    fun testRead() {
        // TODO: Test higher dimensions.
        val reader = NetcdfReader(path)
        reader.setCursor(listOf("pr", "tas"))
        val indexes = listOf(listOf(0, 0, 0), listOf(0, 0, 1))
        val records = indexes.map { reader.read(it.toIntArray()) }.toList()
        val time = records.map { it[0] }.toList()
        assertTrue(time.all { it == "2000-05-16T12:00:00Z" })
        val tas = records.map { it[4].toString().toFloat() }.toList()
        assertEquals(listOf(215.8935f, 215.80531f), tas)
    }

    /**
     * Test the rows() method.
     */
    @Test
    fun testRows() {
        val reader = NetcdfReader(path)
        reader.setCursor(listOf("pr", "tas"))
        val rows = reader.rows(1, 3).toList()
        val time = rows.map { it[0] }.toList()
        assertTrue(time.all { it == "2000-05-16T12:00:00Z" })
        val tas = rows.map { it[4].toString().toFloat() }.toList()
        assertEquals(listOf(215.80531f, 215.73935f), tas)
    }
}
