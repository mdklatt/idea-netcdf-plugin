/**
 * View netCDF file contents.
 *
 * @see: <a href="https://plugins.jetbrains.com/docs/intellij/tool-windows.html">Tool Windows</a>
 */
package software.mdklatt.idea.netcdf.tools

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.wm.*
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.intellij.ui.layout.panel
import com.intellij.ui.table.JBTable
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import java.io.IOException
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.table.DefaultTableModel


/**
 * NetCDF viewer display.
 */
class NetcdfToolWindow: ToolWindowFactory, DumbAware {
    /**
     * File schema content.
     */
    inner class SchemaTab : JBTable(DefaultTableModel()) {
        private val logger = Logger.getInstance(this::class.java)  // runtime class resolution

        init {
            emptyText.text = "Drop netCDF file here to open"
            dropTarget = createDropTarget()
            setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
            selectionModel.addListSelectionListener(this::selectionListener)
        }

        /**
         * Create a drag-and-drop target for opening a netCDF file.
         *
         * @return: DropTarget instance
         */
        private fun createDropTarget(): DropTarget {
            return object : DropTarget() {
                @Synchronized
                override fun drop(event: DropTargetDropEvent) {
                    try {
                        event.acceptDrop(DnDConstants.ACTION_COPY)
                        val accepted = event.transferable.getTransferData(DataFlavor.javaFileListFlavor)
                        val file = (accepted as? List<*>)?.get(0) as File
                        reader.open(file.path)
                        load()
                        varNames = emptyList()
                        displayedVarNames = emptyList()
                    } catch (_: UnsupportedFlavorException) {
                        JOptionPane.showMessageDialog(null, "Unable to read file")
                    } catch (_: IOException) {
                        JOptionPane.showMessageDialog(null, "Unable to read file")
                    }
                    return
                }
            }
        }

        /**
         * Handle row selection events.
         */
        private fun selectionListener(event: ListSelectionEvent?) {
            // TODO: Does event need to be validated?
            if (event == null || selectionModel.isSelectionEmpty) {
                return
            }
            varNames = selectedRows.map { reader.schema.keys.toList()[it] }.toList()
            val dimensionString = reader.schema[varNames.first()]?.get("dimensions")
            if (!varNames.all { reader.schema[it]?.get("dimensions") == dimensionString }) {
                ErrorDialog("Selected variables must have the same dimensions").showAndGet()
                val index = selectionModel.anchorSelectionIndex
                selectionModel.removeSelectionInterval(index, index)
                return
            }
        }

        /**
         * Load data for display.
         */
        private fun load() {
            val model = this.model as DefaultTableModel
            model.setDataVector(emptyArray(), emptyArray())
            val columns = arrayOf("Variable", "Description", "Dimensions", "Units", "Type")
            model.setColumnIdentifiers(columns)
            reader.schema.entries.forEach {
                model.addRow(arrayOf(
                    it.key,
                    it.value["description"],
                    it.value["dimensions"],
                    it.value["units"],
                    it.value["type"],
                ))
            }
            return
        }
    }

    /**
     * Variable data content.
     */
    inner class DataTab : JBTable(DefaultTableModel()) {
        init {
            emptyText.text = "Select variable(s) in Schema tab"
            autoCreateRowSorter = true
        }

        /**
         * Load variables from the netCDF file.
         */
        internal fun load() {
            if (displayedVarNames.isNotEmpty() && displayedVarNames == varNames) {
                return  // selected variables are already displayed
            }
            reader.setCursor(varNames)
            val model = this.model as DefaultTableModel
            model.setDataVector(emptyArray(), emptyArray())
            model.setColumnIdentifiers(reader.columns)
            val maxRows = 1000  // TODO: use pagination
            reader.indexes().take(maxRows).forEach { model.addRow(reader.read(it)) }
            displayedVarNames = varNames
            return
        }
    }

    private var displayedVarNames = emptyList<String>()
    private var reader = NetcdfReader()
    private var schemaTab = SchemaTab()
    private var dataTab = DataTab()
    private var varNames = emptyList<String>()


    /**
     * Create tool window content.
     *
     * @param project: current project
     * @param window current tool window
     */
    override fun createToolWindowContent(project: Project, window: ToolWindow) {
        window.addContentManagerListener(object : ContentManagerListener {
            override fun selectionChanged(event: ContentManagerEvent) {
                super.selectionChanged(event)
                event.let {
                    if (it.index == 1 && it.operation.name == "add") {
                        // Data tab is selected, load data.
                        // TODO: Don't hard code the index.
                        dataTab.load()
                    }
                }
            }
        })
        val factory = window.contentManager.factory
        factory.createContent(JBScrollPane(schemaTab), "Schema", false).let {
            it.description = "File schema"
            window.contentManager.addContent(it)
        }
        factory.createContent(JBScrollPane(dataTab), "Data", false).let {
            it.description = "Selected variables"
            window.contentManager.addContent(it)
        }
        return
    }
}


/**
 * Modal dialog for an error message.
 */
private class ErrorDialog(private val message: String) : DialogWrapper(false) {

    init {
        init()
        title = "Error"
    }

    /**
     * Define dialog contents.
     *
     * @return: dialog contents
     */
    protected override fun createCenterPanel(): JComponent {
        // https://www.jetbrains.org/intellij/sdk/docs/user_interface_components/kotlin_ui_dsl.html
        return panel {
            row(message) {}
        }
    }
}
