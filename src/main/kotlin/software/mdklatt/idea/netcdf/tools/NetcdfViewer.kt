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
import com.intellij.ui.content.*
import com.intellij.ui.layout.panel
import com.intellij.ui.table.JBTable
import com.intellij.ui.treeStructure.Tree
import ucar.nc2.*
import java.awt.Font
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import java.io.IOException
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import kotlin.sequences.Sequence


/**
 * NetCDF viewer display.
 */
class NetcdfToolWindow: ToolWindowFactory, DumbAware {

    /**
     * Interface for tool window content tabs.
     */
    private interface ContentTab {
        val title: String
        val description: String
        fun component(): JComponent
        fun load()
    }


    /**
     * File schema content.
     */
    inner class SchemaTab : JBTable(SchemaTableModel()), ContentTab {
        private val logger = Logger.getInstance(this::class.java)  // runtime class resolution

        override val title = "Schema"
        override val description = "File schema"
        override fun component() = JBScrollPane(this)

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
                        ncFile?.close()
                        ncFile = NetcdfFile.open(file.path)
                        load()
                        selectedVars = emptyList()
                        displayedVars = emptyList()
                    } catch (_: UnsupportedFlavorException) {
                        JOptionPane.showMessageDialog(null, "Unable to read file")
                    } catch (_: IOException) {
                        JOptionPane.showMessageDialog(null, "Could not open netCDF file")
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
            val model = this.model as SchemaTableModel
            val dimensions = selectedRows.map { model.getValueAt(it, 2) }.toSet()
            if (dimensions.size > 1) {
                ErrorDialog("Selected variables must have the same dimensions").showAndGet()
                val index = selectionModel.anchorSelectionIndex
                selectionModel.removeSelectionInterval(index, index)
                return
            }
            selectedVars = selectedRows.map { model.getValueAt(it, 0) }.toList()
        }

        /**
         * Load data for display.
         */
        override fun load() {
            (model as SchemaTableModel).setData(ncFile!!)
            formatColumns()
            return
        }

        /**
         * Set column formatting.
         */
        private fun formatColumns() {
            columnModel.columns.asSequence().forEach() {
                it.headerRenderer = object: DefaultTableCellRenderer() {
                    // Set column labels to bold.
                    override fun setFont(font: Font?) {
                        super.setFont(font?.deriveFont(Font.BOLD))
                    }
                }
            }
            return
        }
    }

    /**
     * Variable data content.
     */
    inner class DataTab : JBTable(DataTableModel()), ContentTab {

        override val title = "Data"
        override val description = "Selected variables"
        override fun component() = JBScrollPane(this)

        init {
            emptyText.text = "Select variable(s) in Schema tab"
            autoCreateRowSorter = true
        }

        /**
         * Load variables from the netCDF file.
         */
        override fun load() {
            if (displayedVars == selectedVars) {
                return  // selected variables are already displayed
            }
            (model as DataTableModel).setData(ncFile!!, selectedVars.asSequence())
            displayedVars = selectedVars
            formatColumns()
            return
        }

        /**
         * Set column formatting.
         */
        private fun formatColumns() {
            columnModel.columns.asSequence().forEach() {
                var headerStyle = Font.BOLD
                var cellStyle = Font.PLAIN
                if (!selectedVars.contains(it.headerValue)) {
                    // Add italics to coordinate columns.
                    headerStyle = headerStyle or Font.ITALIC
                    cellStyle = cellStyle or Font.ITALIC
                }
                it.headerRenderer = object: DefaultTableCellRenderer() {
                    // Set header style.
                    override fun setFont(font: Font?) {
                        super.setFont(font?.deriveFont(headerStyle))
                    }
                }
                it.cellRenderer = object: DefaultTableCellRenderer() {
                    // Set regular cell style.
                    override fun setFont(font: Font?) {
                        super.setFont(font?.deriveFont(cellStyle))
                    }
                }
            }
            return
        }
    }

    /**
     * File schema tree view (experimental).
     */
    inner class TreeTab : Tree(), ContentTab {

        override val title = "Tree"
        override val description = "File schema (experimental tree view)"
        override fun component() = JBScrollPane(this)

        private var root: DefaultMutableTreeNode? = null

        init {
            emptyText.text = "Drop netCDF file in Schema tab to open"
        }

        override fun load() {
            ncFile?.let { file ->
                root = DefaultMutableTreeNode(ncFile?.location).also {
                    model = DefaultTreeModel(it)
                    addGroup(it, file.rootGroup)
                    expandPath(TreePath(it))
                }
            }
            return
        }

        private fun addGroup(head: DefaultMutableTreeNode, group: Group) {
            val node : DefaultMutableTreeNode
            if (group.isRoot) {
                node = head
            } else {
                node = DefaultMutableTreeNode(group.fullNameEscaped)
                head.add(node)
            }
            addAttributes(node, group.attributes)
            addDimensions(node, group.dimensions)
            addVariables(node, group.variables)
            if (group.groups.isNotEmpty()) {
                DefaultMutableTreeNode("Groups").let {
                    node.add(it)
                    group.groups.forEach { sub-> addGroup(it, sub) }
                }
            }
            return
        }

        private fun addVariables(head: DefaultMutableTreeNode, items: List<Variable>) {
            if (items.isEmpty()) {
                return
            }
            DefaultMutableTreeNode("Variables").let { node ->
                head.add(node)
                items.forEach {
                    // TODO: Is there a reason not to alpha sort?
                    DefaultMutableTreeNode(it.fullNameEscaped).apply {
                        node.add(this)
                        addAttributes(this, it.attributes)
                        addDimensions(this, it.dimensions)
                    }
                }
            }
            return
        }

        private fun addAttributes(head: DefaultMutableTreeNode, items: List<Attribute>) {
            if (items.isEmpty()) {
                return
            }
            DefaultMutableTreeNode("Attributes").let { node ->
                head.add(node)
                items.forEach {
                    // TODO: Alpha sort.
                    val text = "${it.fullNameEscaped}: ${it.stringValue}"
                    node.add(DefaultMutableTreeNode(text))
                }
            }
            return
        }

        private fun addDimensions(head: DefaultMutableTreeNode, items: List<Dimension>) {
            if (items.isEmpty()) {
                return
            }
            DefaultMutableTreeNode("Dimensions").let { node ->
                head.add(node)
                items.forEach {
                    // TODO: Add shape and unlimited.
                    node.add(DefaultMutableTreeNode(it.fullNameEscaped))
                }
            }
            return
        }
    }


    private var tabs = listOf<ContentTab>(SchemaTab(), DataTab(), TreeTab())
    private var ncFile: NetcdfFile? = null
    private var selectedVars = emptyList<String>()
    private var displayedVars = emptyList<String>()


    /**
     * Create tool window content.
     *
     * @param project: current project
     * @param window current tool window
     */
    override fun createToolWindowContent(project: Project, window: ToolWindow) {
        val factory = window.contentManager.factory
        tabs.forEach { tab ->
            factory.createContent(tab.component(), tab.title, false).also {
                it.description = tab.description
                window.contentManager.addContent(it)
            }
        }
        window.addContentManagerListener(object : ContentManagerListener {
            override fun selectionChanged(event: ContentManagerEvent) {
                // Lazy loading when tab is selected.
                super.selectionChanged(event)
                val lazyTabs = listOf("Data", "Tree")
                val eventTab = tabs[event.index]
                if (event.operation.name == "add" && lazyTabs.contains(eventTab.title)) {
                    eventTab.load()
                }
            }
        })
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
    override fun createCenterPanel(): JComponent {
        // https://www.jetbrains.org/intellij/sdk/docs/user_interface_components/kotlin_ui_dsl.html
        return panel {
            row(message) {}
        }
    }
}


internal class DataTableModel() : AbstractTableModel() {

    private val logger = Logger.getInstance(this::class.java)  // runtime class resolution
    var table : TableView? = null

    /**
     * Returns the number of rows in the model. A
     * `JTable` uses this method to determine how many rows it
     * should display.  This method should be quick, as it
     * is called frequently during rendering.
     *
     * @return the number of rows in the model
     * @see .getColumnCount
     */
    override fun getRowCount() = table?.rowCount ?: 0

    /**
     * Returns the number of columns in the model. A
     * `JTable` uses this method to determine how many columns it
     * should create and display by default.
     *
     * @return the number of columns in the model
     * @see .getRowCount
     */
    override fun getColumnCount() = table?.columnCount ?: 0

    /**
     *
     */
    override fun getColumnClass(columnIndex: Int): Class<*> = table?.column(columnIndex)?.type ?: throw IllegalStateException("empty table")

    /**
     * Get the name label for a column.
     *
     */
    override fun getColumnName(column: Int) = table?.column(column)?.label ?: throw IllegalStateException("empty table")

    /**
     * Returns the value for the cell at `columnIndex` and
     * `rowIndex`.
     *
     * @param   rowIndex        the row whose value is to be queried
     * @param   columnIndex     the column whose value is to be queried
     * @return  the value Object at the specified cell
     */
    override fun getValueAt(rowIndex: Int, columnIndex: Int) = table?.column(columnIndex)?.value(rowIndex) ?: throw IllegalStateException("empty table")

    /**
     * Set the model data.
     *
     * The model defines columns consisting of one or more netCDF variables and
     * their corresponding dimension coordinates. All selected variable must
     * have congruent dimensions.
     *
     * @param file: open netCDF file
     * @param varNames: variable names to use
     */
    fun setData(file: NetcdfFile, varNames: Sequence<String>) {
        // TODO: Verify that all variables have the same dimensions.
        logger.debug("Loading data from ${file.location}")
        table?.clear()
        table = TableView(file).also { it.add(varNames) }
        fireTableStructureChanged()
        return
    }
}


/**
 * Table model for a netCDF file schema.
 */
internal class SchemaTableModel : AbstractTableModel() {

    private val logger = Logger.getInstance(this::class.java)  // runtime class resolution
    private val labels = arrayOf("Variable", "Description", "Dimensions", "Units", "Type")
    private var schema = emptyList<Array<String>>()

    /**
     * Returns the number of rows in the model. A
     * `JTable` uses this method to determine how many rows it
     * should display. This method should be quick, as it
     * is called frequently during rendering.
     *
     * @return the number of rows in the model
     * @see .getColumnCount
     */
    override fun getRowCount() = schema.size

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
     *
     */
    override fun getColumnClass(columnIndex: Int): Class<*> {
        return schema[columnIndex].firstOrNull()?.javaClass ?: throw IllegalArgumentException("Empty data model")
    }

    /**
     * Get the name label for a column.
     */
    override fun getColumnName(column: Int) = labels[column]

    /**
     * Returns the value for the cell at `columnIndex` and
     * `rowIndex`.
     *
     * @param   rowIndex        the row whose value is to be queried
     * @param   columnIndex     the column whose value is to be queried
     * @return  the value Object at the specified cell
     */
    override fun getValueAt(rowIndex: Int, columnIndex: Int) = schema[rowIndex][columnIndex]

    /**
     * Set model data.
     *
     * The model defines columns consisting of variable metadata fields.
     *
     * @param file: open netCDF file
     */
    fun setData(file: NetcdfFile) {
        logger.debug("Loading schema from ${file.location}")
        schema = file.variables.map {
            arrayOf(
                it.fullNameEscaped,
                it.description,
                it.nameAndDimensions.substring(it.nameAndDimensions.lastIndexOf("(")),
                it.unitsString,
                it.dataType.name.toLowerCase(),
            )
        }.toList()
        fireTableStructureChanged()
    }
}
