/**
 * Network Common Data Format (netCDF) file support.
 *
 * @see: <a href="https://www.unidata.ucar.edu/software/netcdf">netCDF</a>
 */
package dev.mdklatt.idea.netcdf.files

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.UserBinaryFileType


/**
 * NetCDF file type.
 *
 * @see: <a href="https://plugins.jetbrains.com/docs/intellij/registering-file-type.html">Registering a File Type</a>
 */
class NetcdfFileType: UserBinaryFileType() {

    companion object {
        val INSTANCE = NetcdfFileType()
    }

    /**
     * File type name.
     *
     * This must match <fileType name="..."/> in plugin.xml.
     *
     * @return: name
     */
    override fun getName() = "netCDF"

    /**
     * Default file extension.
     *
     * @return: extension
     */
    override fun getDefaultExtension() = "nc"

    /**
     * File type description.
     *
     * @return: description
     */
    override fun getDescription() = "Network Common Data Format (netCDF)"

    /**
     * Get the 16x16 file type icon.
     *
     * @return: icon
     * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/reference_guide/work_with_icons_and_images.html">Icons and Images</a>
     */
    override fun getIcon() = AllIcons.FileTypes.Custom
}
