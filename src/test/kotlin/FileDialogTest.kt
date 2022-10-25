/**
 * Test suite for FileDialogTest.kt
 */
package dev.mdklatt.idea.netcdf

import com.intellij.testFramework.fixtures.BasePlatformTestCase


// The IDEA platform tests use JUnit3, so method names are used to determine
// behavior instead of annotations. Notably, test classes are *not* constructed
// before each test, so setUp() methods should be used for initialization.
// Also, test functions must be named `testXXX` or they will not be found
// during automatic discovery.


/**
 * Unit tests for the SaveFileDialog class.
 */
internal class SaveFileDialogTest : BasePlatformTestCase() {

    // TODO: https://github.com/JetBrains/intellij-ui-test-robot

    private lateinit var dialog: SaveFileDialog

    /**
     * Determine if test should be run.
     *
     * @return: true if test should be run
     */
    override fun shouldRunTest(): Boolean {
        if (System.getProperty("os.arch") == "aarch64") {
            // Skip all fixture tests on Apple Silicon due to failure to load
            // JNA library. Not sure if this is the proper way to do this, but
            // it works. Notably, setUp() will no be called, which is where the
            // failure is occurring.
            // TODO: Make tests work on Apple Silicon.
            return false
        }
        return super.shouldRunTest()
    }

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        // FIXME: <https://github.com/mdklatt/idea-netcdf-plugin/issues/1>
        super.setUp()
        dialog = SaveFileDialog("Save File Test")
    }

    /**
     * Test the primary constructor.
     */
    fun testConstructor() {
        assertEquals("Save File Test", dialog.title)
    }
}
