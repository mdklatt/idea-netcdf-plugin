package software.mdklatt.idea.netcdf.tools

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 * Unit tests for the NetcdfReader class.
 */
internal class NetcdfReaderTest {

    private val ncPath = "src/test/resources/sresa1b_ncar_ccsm3-example.nc"
    private val varNames = listOf("pr", "tas")

    /**
     * Test the default constructor.
     */
    @Test
    fun testDefaultConstructor() {
        val reader = NetcdfReader()
        assertTrue(reader.isClosed)
        assertTrue(reader.variables.isEmpty())
    }

    /**
     * Test the open constructor.
     */
    @Test
    fun testOpenConstructor() {
        val reader = NetcdfReader(ncPath)
        assertFalse(reader.isClosed)
        assertTrue(reader.variables.isNotEmpty())
        reader.close()
    }

    /**
     * Test the open() method.
     */
    @Test
    fun testOpen() {
        val reader = NetcdfReader()
        reader.open(ncPath)
        assertFalse(reader.isClosed)
        assertTrue(reader.variables.isNotEmpty())
    }

    /**
     * Test the close() method.
     */
    @Test
    fun testClose() {
        val reader = NetcdfReader(ncPath)
        reader.close()
        reader.close()  // should be safe if reader is already closed
        assertTrue(reader.isClosed)
        assertTrue(reader.variables.isEmpty())
    }

    /**
     * Test the use() method.
     */
    @Test
    fun testUse() {
        val reader = NetcdfReader(ncPath)
        reader.use {
            assertFalse(reader.isClosed)
        }
        assertTrue(reader.isClosed)
    }

    /**
     * Test the variables property.
     */
    @Test
    fun testVariables() {
        NetcdfReader(ncPath).apply {
            // TODO: This is just a sanity check for now.
            assertEquals(12, variables.size)
        }
    }

    /**
     * Test the read() method.
     */
    @Test
    fun testRead() {
        val reader = NetcdfReader(ncPath)
        val tas = reader.read(varNames).take(2).map { it.data["tas"].toString() }
        assertEquals(listOf("215.8935 ", "215.80531 "), tas.toList())
    }
}
