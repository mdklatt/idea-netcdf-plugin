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
import com.intellij.ui.layout.panel
import com.intellij.ui.table.JBTable
import ucar.nc2.Dimension
import ucar.nc2.NetcdfFile
import ucar.nc2.Variable
import vendor.tandrial.itertools.cartProd
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
     * Display the file schema.
     */
    inner class SchemaTab : JBTable(NetcdfTableModel()) {
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
                        ncFile = NetcdfFile.open(file.path)  // TODO: does this throw or return null on failure?
                        logger.info("Opening netCDF file ${ncFile!!.location}")
                        (model as NetcdfTableModel).readSchema(ncFile!!)
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
            val variables = selectedRows.map { ncFile!!.variables[it] }.toTypedArray()
            if (!variables.all { it.dimensionsString == variables[0].dimensionsString }) {
                val message = "Selected variables must have the same dimensions"
                logger.error(message)
                ErrorDialog("Selected variables must have the same dimensions").showAndGet()
                val index = selectionModel.anchorSelectionIndex
                selectionModel.removeSelectionInterval(index, index)
                return
            }
            (dataTab.model as NetcdfTableModel).readData(ncFile!!, variables)
        }
    }

    /**
     * Show variable data.
     */
    inner class DataTab : JBTable(NetcdfTableModel()) {
        init {
            emptyText.text = "Select variable(s) in Schema tab"
        }
    }

    private var ncFile: NetcdfFile? = null
    private var schemaTab = SchemaTab()
    private var dataTab = DataTab()

    /**
     * Create tool window content.
     *
     * @param project: current project
     * @param window current tool window
     */
    override fun createToolWindowContent(project: Project, window: ToolWindow) {
        val factory = window.contentManager.factory
        factory.createContent(JBScrollPane(schemaTab), "Schema", false).let {
            window.contentManager.addContent(it)
        }
        factory.createContent(JBScrollPane(dataTab), "Data", false).let {
            window.contentManager.addContent(it)
        }
        return
    }
}


/**
 * Table data model for netCDF files.
 */
class NetcdfTableModel : DefaultTableModel() {

    private val logger = Logger.getInstance(this::class.java)  // runtime class resolution

    /**
     * Clear table data.
     */
    public fun clear() {
        setDataVector(emptyArray(), emptyArray())
        return
    }

    /**
     * Read a netCDF file schema.
     */
    public fun readSchema(file: NetcdfFile) {
        clear()
        setColumnIdentifiers(arrayOf("Variable", "Description", "Units", "Type"))
        file.variables.forEach {
            addRow(arrayOf(
                it.nameAndDimensions,
                it.description ?: "",
                it.unitsString ?: "",
                it.dataType.name,
            ))
        }
        return
    }

    /**
     * Read netCDF data variables.
     */
    public fun readData(ncFile: NetcdfFile, variables: Array<Variable>) {
        clear()
        val dimensionString = variables.getOrNull(0)?.dimensionsString ?: ""
        if (!variables.all { it.dimensionsString == dimensionString }) {
            val message = "Selected variables must have the same dimensions"
            logger.error(message)
            ErrorDialog("Selected variables must have the same dimensions").showAndGet()
            return
        }
        setColumnIdentifiers(variables.map { it.fullName }.toTypedArray())
        val dimensions = variables[0].dimensions
        val labels = mutableListOf("(${dimensions.map { it.fullName }.joinToString(", ")})")
        assert(labels.addAll(variables.map { it.fullName }))
        setColumnIdentifiers(labels.toTypedArray())
        val rowCoords = cartProd(*dimensions.map { (0 until it.length).toList() }.toTypedArray())
        val rowLabels = cartProd(*dimensions.map { dimensionValues(ncFile, it).toList() }.toTypedArray())
        rowCoords.zip(rowLabels).withIndex().takeWhile { it.index < 10 }.forEach {
            val coords = it.value.first.toIntArray()
            val values = variables.map { variable ->
                val shape = IntArray(variable.rank) { 1 }
                variable.read(coords, shape).toString()
            }
            val row = mutableListOf("(${it.value.second.joinToString(", ")})")
            assert(row.addAll(values))
            addRow(row.toTypedArray())
        }
        return
    }

    /**
     * Get dimension variable values.
     *
     * If there is no matching dimension variable, the values are the dimension
     * indexes.
     *
     * @param ncFile:
     * @param dimension:
     * @return
     */
    private fun dimensionValues(ncFile: NetcdfFile, dimension: Dimension): Array<String> {
        val dimensionVar = ncFile.findVariable(dimension.fullName)
        val values = mutableListOf<String>()
        if (dimensionVar != null) {
            if (dimensionVar.rank != 1) {
                throw RuntimeException("dimension variable does not have rank of 1")
            }
            dimensionVar.read().indexIterator.apply {
                while (hasNext()) {
                    values.add(objectNext.toString())
                }
            }
        } else {
            values.addAll((0 until dimension.length).map { it.toString() })
        }
        return values.toTypedArray()
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
