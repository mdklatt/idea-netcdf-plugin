package dev.mdklatt.idea.netcdf.actions

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File
import kotlin.io.path.createTempFile
import kotlin.test.assertContentEquals


/**
 * Unit tests for the WriteSchemaAction classes.
 */
internal class WriteSchemaActionTest : BasePlatformTestCase() {

private val ncPath = "src/test/resources/sresa1b_ncar_ccsm3-example.nc"

    /**
     * Test the writeSchema() method for CDL output.
     */
    fun testWriteSchema() {
        sequenceOf(
            // Poor man's @ParametrizedTest because of JUnit3.
            Pair(WriteCdlAction(), ".cdl"),
            Pair(WriteNcmlAction(), ".ncml"),
        ).forEach { (action, ext) ->
            var expect: ByteArray
            File(ncPath.replace(".nc", ext)).inputStream().use {
                expect = it.readAllBytes()
            }
            val outPath = createTempFile(suffix = ext)
            action.writeSchema(ncPath, outPath.toString())
            outPath.toFile().inputStream().use {
                // Compare the ends of the files because the beginning contains
                // the location of the file.
                val actual = it.readAllBytes()
                val count = actual.size - 200  // skip file-specific content
                assertContentEquals(expect.takeLast(count), actual.takeLast(count))
            }
        }
    }
}
