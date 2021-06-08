/**
 * Unit tests for the NetcdfViewer module.
 */
package software.mdklatt.idea.netcdf.tools

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.jupiter.api.Test
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
 * Test the TableModel class.
 *
 * This is an abstract class; a sample implementation is tested here.
 */
internal class TableModelTest {  // JUnit5

    private val model = object : TableModel() {
        override val labels = arrayOf("first", "last")
        override val records = arrayListOf(
            arrayOf<Any>("one", 1),
            arrayOf<Any>("two", 2),
        )
    }

    /**
     * Test the rowCount attribute.
     */
    @Test
    fun testRowCount() {
        assertEquals(2, model.rowCount)
    }

    /**
     * Test the columnCount attribute.
     */
    @Test
    fun testColumnCount() {
        assertEquals(2, model.columnCount)
    }

    /**
     * Test the getColumnName() method.
     */
    @Test
    fun testGetColumnName() {
        assertEquals("first", model.getColumnName(0))
        assertEquals("last", model.getColumnName(1))
    }

    /**
     * Test the getValueAt() method.
     */
    @Test
    fun testGetValueAt() {
        assertEquals("one", model.getValueAt(0, 0))
        assertEquals(2, model.getValueAt(1, 1))
    }
}
