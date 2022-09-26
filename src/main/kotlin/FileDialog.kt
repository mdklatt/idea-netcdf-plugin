package dev.mdklatt.idea.netcdf

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent


/**
 * Modal dialog for an output file path.
 */
class SaveFileDialog(title: String = "Save File", private val prompt: String = "File", default: String = "") :
    DialogWrapper(false) {

    private var path = default

    init {
        init()
        setTitle(title)
    }

    /**
     * Prompt the user for the password.
     *
     * @return user input
     */
    fun getPath(): String? = if (showAndGet()) path else null

    /**
     * Define dialog contents.
     *
     * @return: dialog contents
     */
    override fun createCenterPanel(): JComponent {
        // https://plugins.jetbrains.com/docs/intellij/kotlin-ui-dsl-version-2.html
        return panel{
            row("${prompt}:") {
                textFieldWithBrowseButton("File").columns(40).bindText(::path)
            }
        }
    }
}
