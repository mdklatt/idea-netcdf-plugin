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
