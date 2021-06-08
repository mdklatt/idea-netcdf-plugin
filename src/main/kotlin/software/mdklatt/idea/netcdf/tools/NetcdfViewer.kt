/**
 * View netCDF file contents.
 *
 * @see: <a href="https://plugins.jetbrains.com/docs/intellij/tool-windows.html">Tool Windows</a>
 */

package software.mdklatt.idea.netcdf.tools

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.*
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import ucar.nc2.NetcdfFile
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import java.io.IOException
import javax.swing.JOptionPane
import javax.swing.table.AbstractTableModel


/**
 * NetCDF viewer display.
 */
class NetcdfToolWindow: ToolWindowFactory, DumbAware {

    private class SchemaTab(internal val model: SchemaModel) : JBTable(model) {

        private val logger = Logger.getInstance(this::class.java)  // runtime class resolution

        init {
            emptyText.text = "Drop netCDF file here to open"
            dropTarget = createDropTarget()
        }

        private fun createDropTarget(): DropTarget {
            return object : DropTarget() {
                @Synchronized
                override fun drop(event: DropTargetDropEvent) {
                    try {
                        // tabbedPane.setSelectedIndex(0)
                        event.acceptDrop(DnDConstants.ACTION_COPY)
                        val file = (event.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>)[0]
                        model.readSchema(NetcdfFile.open(file.path))
                        logger.info("opening file ${file.path}")
                    } catch (e: UnsupportedFlavorException) {
                        JOptionPane.showMessageDialog(null, "Unable to read file")
                    } catch (e: IOException) {
                        JOptionPane.showMessageDialog(null, "Unable to read file")
                    }
                }
            }
        }

    }

    private var schemaTab = SchemaTab(SchemaModel())
    private var dataTab = JBTable().apply {
        // TODO: disable unless file is open in schema tab
        emptyText.text = "No variable(s) selected"
    }



    /**
     * Create tool window content.
     *
     * @param project: current project
     * @param window current tool window
     */
    override fun createToolWindowContent(project: Project, window: ToolWindow) {
        val factory = window.contentManager.factory
        window.contentManager.addContent(factory.createContent(JBScrollPane(schemaTab), "Schema", false))
        window.contentManager.addContent(factory.createContent(dataTab, "Data", false))
        return
    }

}


/**
 * Data model for the Schema content tab.
 */
private class SchemaModel: TableModel() {

    override val labels = arrayOf("Variable", "Description", "Units", "Type")
    override val records = arrayListOf<Array<Any>>()

    internal fun readSchema(file: NetcdfFile) {
        file.use {
            for (variable in it.variables) {
                records.add(arrayOf(
                    variable.nameAndDimensions,
                    variable.description ?: "",
                    variable.unitsString ?: "",
                    variable.dataType.name,
                ))
            }
        }
    }
}


/**
 * Basic data model for NetcdfViewer table elements.
 */
internal abstract class TableModel: AbstractTableModel() {

    protected abstract val labels: Array<String>
    protected open val records = arrayListOf<Array<Any>>()

    /**
     * Return the name of a given column.
     *
     * This overrides the default implementation which uses lettered columns,
     * 'A', 'B', 'C', etc.
     *
     * @return: column name
     */
    override fun getColumnName(column: Int) = labels[column]

    /**
     * Returns the number of rows in the model. A
     * `JTable` uses this method to determine how many rows it
     * should display.  This method should be quick, as it
     * is called frequently during rendering.
     *
     * @return the number of rows in the model
     * @see .getColumnCount
     */
    override fun getRowCount() = records.size

    /**
     * Returns the number of columns in the model. A
     * `JTable` uses this method to determine how many columns it
     * should create and display by default.
     *
     * @return the number of columns in the model
     * @see .getRowCount
     */
    override fun getColumnCount() = labels.size

    /**
     * Returns the value for the cell at `columnIndex` and
     * `rowIndex`.
     *
     * @param   rowIndex        the row whose value is to be queried
     * @param   columnIndex     the column whose value is to be queried
     * @return  the value Object at the specified cell
     */
    override fun getValueAt(rowIndex: Int, columnIndex: Int) = records[rowIndex][columnIndex]
}
