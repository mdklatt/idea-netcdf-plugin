/**
 * NetCDF Markup Language (NcML) files.
 *
 * @see <a href="https://www.unidata.ucar.edu/software/netcdf-java/v4.6/ncml"></a>
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
import ucar.nc2.NetcdfFiles
import java.io.File
import java.lang.IllegalArgumentException

/**
 * NCML file type.
 *
 * @see: <a href="https://plugins.jetbrains.com/docs/intellij/registering-file-type.html">Registering a File Type</a>
 */
class NcmlFileType: UserFileType<NcmlFileType>() {  // TODO: LanguageFileType

    companion object {
        val INSTANCE = NcmlFileType()
    }

    /**
     * File type name.
     *
     * This must match <fileType name="..."/> in plugin.xml.
     *
     * @return: name
     */
    override fun getName() = "NcML"

    /**
     * Default file extension.
     *
     * @return extension
     */
    override fun getDefaultExtension() = "ncml"

    /**
     * File type description.
     *
     * @return description
     */
    override fun getDescription() = "NetCDF Markup Language (NcML)"

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
     * Get an editor for this file type.
     */
    override fun getEditor(): SettingsEditor<NcmlFileType> {
        TODO("Not yet implemented")
    }
}


/**
 * Write netCDF file schema to NcML.
 */
class WriteNcmlFileAction : AnAction() {

    private val logger = Logger.getInstance(this::class.java)  // runtime class resolution

    /**
     * Execute the action.
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
     * Write file schema to an NcML file.
     *
     * @param ncPath: input netCDF file
     * @param ncmlPath: output NcML file (prompt user by default)
     */
    internal fun writeSchema(ncPath: String, ncmlPath: String = "") {
        val _ncmlPath = ncmlPath.ifEmpty {
            // Prompt user for CDL output path.
            val default = ncPath.replace(".nc", ".ncml")  // FIXME: not robust
            val dialog = SaveFileDialog("Save NcML File", default = default)
            dialog.getPath() ?: throw IllegalArgumentException("No path selected")
        }
        File(_ncmlPath).printWriter().use {
            logger.debug("Writing to NcML file $_ncmlPath")
            val ncFile = NetcdfFiles.open(ncPath)
            ncFile.writeNcml(it, null)
        }
    }
}
