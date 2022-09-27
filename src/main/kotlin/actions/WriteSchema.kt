/**
 * Generate schema files from netCDF files.
 */
package dev.mdklatt.idea.netcdf.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileTypes.UserFileType
import com.intellij.openapi.ui.Messages
import dev.mdklatt.idea.netcdf.SaveFileDialog
import dev.mdklatt.idea.netcdf.files.CdlFileType
import dev.mdklatt.idea.netcdf.files.NcmlFileType
import dev.mdklatt.idea.netcdf.files.NetcdfFileType
import ucar.nc2.NetcdfFiles
import ucar.nc2.write.CDLWriter
import java.io.File
import java.util.*


/**
 * Write netCDF schema to a file.
 */
abstract class WriteSchemaAction(private val type: UserFileType<*>): AnAction() {

    protected val logger = Logger.getInstance(this::class.java)  // runtime class resolution

    /**
     * Execute the action.
     *
     * @param event Carries information on the invocation place
     */
    override fun actionPerformed(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.PSI_FILE)?.containingFile
        val ncPath = file?.virtualFile?.canonicalPath
        if (file?.fileType is NetcdfFileType && ncPath != null) {
            val outPath = schemaFilePrompt(ncPath) ?: return  // dialog cancelled
            writeSchema(ncPath, outPath)
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
     * Prompt the user for the output file path using a modal dialog.
     *
     * @param ncPath: netCDF input file path
     */
    private fun schemaFilePrompt(ncPath: String): String? {
        val outPath = ncPath.replace(".nc", ".${type.defaultExtension}")  // TODO: not robust
        val dialog = SaveFileDialog("Save ${type.name} File", default = outPath)
        return dialog.getPath()
    }

    /**
     * Write netCDF schema to a file.
     *
     * @param ncPath: input netCDF file
     * @param outPath: output NcML file (prompt user by default)
     */
    internal abstract fun writeSchema(ncPath: String, outPath: String)

}


/**
 * Write netCDF file schema as CDL.
 */
internal class WriteCdlAction : WriteSchemaAction(CdlFileType()) {

    /**
     * Write file schema to a CDL file.
     *
     * @param ncPath: input netCDF file
     * @param outPath: output NcML file (prompt user by default)
     */
    internal override fun writeSchema(ncPath: String, outPath: String ) {
        NetcdfFiles.open(ncPath).use { ncFile ->
            Formatter(outPath, "UTF-8").use {
                logger.debug("Writing to CDL file $outPath")
                CDLWriter.writeCDL(ncFile, it, true, outPath)
            }
        }
    }
}


/**
 * Write netCDF file schema as NcML.
 */
internal class WriteNcmlAction : WriteSchemaAction(NcmlFileType()) {
    /**
     * Write file schema to an NcML file.
     *
     * @param ncPath: input netCDF file
     * @param outPath: output NcML file (prompt user by default)
     */
    internal override fun writeSchema(ncPath: String, outPath: String ) {
        NetcdfFiles.open(ncPath).use { ncFile ->
            File(outPath).printWriter().use {
                logger.debug("Writing to CDL file $outPath")
                ncFile.writeNcml(it, null)
            }
        }
    }
}
