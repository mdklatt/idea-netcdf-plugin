/**
 * Implementation of the "Open NetCDF File Viewer" action.
 *
 * @see: <a href="https://plugins.jetbrains.com/docs/intellij/basic-action-system.html">Actions</a>
 */
package software.mdklatt.idea.netcdf.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import software.mdklatt.idea.netcdf.files.NetcdfFileType
import software.mdklatt.idea.netcdf.tools.NetcdfViewer


/**
 * Handler for the "Open NetCDF Viewer" action.
 */
class OpenNetcdfViewer : AnAction() {
    /**
     * Load the selected netCDF file into the tool window.
     *
     * @param event: action event
     */
    override fun actionPerformed(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.PSI_FILE)?.containingFile
        val path = file?.virtualFile?.canonicalPath
        if (file?.fileType is NetcdfFileType && path != null) {
            // This is an action in the Project View window, so presumably the
            // project reference is never null...?
            NetcdfViewer(event.project!!).also {
                it.open(path)
            }
        } else {
            Messages.showMessageDialog(
                event.project,
                "Not a netCDF file",
                event.presentation.text,
                Messages.getErrorIcon()
            )
        }
        return
    }
}
