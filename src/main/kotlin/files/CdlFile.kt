/**
 * Common Data Language (CDL) file support.
 */
package dev.mdklatt.idea.netcdf.files

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.UserFileType
import com.intellij.openapi.options.SettingsEditor


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
     * Get an editor for this file type.
     */
    override fun getEditor(): SettingsEditor<CdlFileType> {
        TODO("Not yet implemented")
    }
}
