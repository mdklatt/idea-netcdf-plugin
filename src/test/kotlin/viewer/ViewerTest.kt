/**
 * Unit tests for Viewer.kt.
 */
package dev.mdklatt.idea.netcdf.viewer

import com.intellij.openapi.actionSystem.AnActionEvent
import org.junit.jupiter.api.AfterEach
import ucar.nc2.NetcdfFile
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.Test


/**
 * Unit tests for ViewerTab classes.
 */
internal class ViewerTabTest {

    private val path = "src/test/resources/sresa1b_ncar_ccsm3-example.nc"
    private val fileTab = FileTab(path)

    /**
     * Per-test clean up.
     */
    @AfterEach
    fun tearDown() {
        fileTab.dispose()
    }

    @Test
    fun testFileTab() {
        assertEquals(path, fileTab.file.location)
        assertTrue(fileTab.selectedVars.isEmpty())
        assertNotNull(fileTab.model)
        assertNotNull(fileTab)
    }

    @Test
    fun testDataTab() {
        val dataTab = DataTab(fileTab)
        assertNotNull(dataTab.model)
        dataTab.dispose()
    }
}
