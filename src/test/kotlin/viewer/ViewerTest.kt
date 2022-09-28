/**
 * Unit tests for Viewer.kt.
 */
package dev.mdklatt.idea.netcdf.viewer

import org.junit.jupiter.api.AfterEach
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.Test


/**
 * Unit tests for ViewerTab classes.
 */
internal class ViewerTabTest {

    private val path = "src/test/resources/sresa1b_ncar_ccsm3-example.nc"
    private val schemaTab = SchemaTab(path)

    /**
     * Per-test clean up.
     */
    @AfterEach
    fun tearDown() {
        schemaTab.dispose()
    }

    @Test
    fun testFileTab() {
        assertEquals(path, schemaTab.file.location)
        assertTrue(schemaTab.selectedVars.isEmpty())
        assertNotNull(schemaTab.model)
        assertNotNull(schemaTab)
    }

    @Test
    fun testDataTab() {
        val dataTab = DataTab(schemaTab)
        assertNotNull(dataTab.model)
        dataTab.dispose()
    }
}
