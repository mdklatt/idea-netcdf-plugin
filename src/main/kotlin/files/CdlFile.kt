/**
 * Common Data Language (CDL) file support.
 */
package dev.mdklatt.idea.netcdf.files

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileTypes.UserFileType
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.Messages
import dev.mdklatt.idea.netcdf.SaveFileDialog
import java.util.Formatter
import ucar.nc2.NetcdfFiles
import ucar.nc2.write.CDLWriter
import java.lang.IllegalArgumentException


/**
 * CDL file type.
 *
 * @see: <a href="https://plugins.jetbrains.com/docs/intellij/registering-file-type.html">Registering a File Type</a>
 */
class CdlFileType: UserFileType<CdlFileType>() {  // TODO: LanguageFileType

    companion object {
        val INSTANCE = CdlFileType()
    }

    /**
     * File type name.
     *
     * This must match <fileType name="..."/> in plugin.xml.
     *
     * @return: name
     */
    override fun getName() = "CDL"

    /**
     * Default file extension.
     *
     * @return extension
     */
    override fun getDefaultExtension() = "cdl"

    /**
     * File type description.
     *
     * @return description
     */
    override fun getDescription() = "Common Data Language (CDL)"

    /**
     * Returns the 16x16 icon used to represent the file type.
     *
     * @return: file icon
     * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/reference_guide/work_with_icons_and_images.html">Icons and Images</a>
     */
    override fun getIcon() = AllIcons.FileTypes.Text

    /**
     * Determine if file contains binary content.
     *
     * @return `false`
     */
    override fun isBinary() = false

    /**
     *
     */
    override fun getEditor(): SettingsEditor<CdlFileType> {
        TODO("Not yet implemented")
    }
}


/**
 *
 */
class WriteCdlFileAction : AnAction() {

    private val logger = Logger.getInstance(this::class.java)  // runtime class resolution

    /**
     * Write a netCDF file schema to a CDL file.
     *
     * @param event Carries information on the invocation place
     */
    override fun actionPerformed(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.PSI_FILE)?.containingFile
        val path = file?.virtualFile?.canonicalPath
        if (file?.fileType is NetcdfFileType && path != null) {
            writeSchema(path)
        } else {
            Messages.showMessageDialog(
                event.project,
                "Not a netCDF file",
                event.presentation.text,
                Messages.getErrorIcon()
            )
        }
    }

    /**
     * Write a netCDF file schema to a CDL file.
     *
     * @param ncPath: input netCDF file
     * @param cdlPath: output CDL file (prompt user by default)
     */
    internal fun writeSchema(ncPath: String, cdlPath: String = "") {
        val ncFile = NetcdfFiles.open(ncPath)
        val _cdlPath = cdlPath.ifEmpty {
            // Prompt user for CDL output path.
            val default = ncPath.replace(".nc", ".cdl")  // FIXME: not robust
            val dialog = SaveFileDialog("Save CDL File", default = default)
            dialog.getPath() ?: throw IllegalArgumentException("No path selected")
        }
        val formatter = Formatter(_cdlPath, "UTF-8")
        CDLWriter.writeCDL(ncFile, formatter, true, _cdlPath)
        logger.warn("Creating CDL file $_cdlPath")
        formatter.close()
    }
}
