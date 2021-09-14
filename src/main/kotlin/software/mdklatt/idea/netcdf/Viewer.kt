/**
 * Implementation of the NetCDF Viewer tool.
 */
package software.mdklatt.idea.netcdf

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.intellij.ui.table.JBTable
import com.intellij.ui.treeStructure.Tree
import software.mdklatt.idea.netcdf.files.NetcdfFileType
import ucar.nc2.*
import java.awt.Font
import javax.swing.JComponent
import javax.swing.ListSelectionModel
import javax.swing.event.ListSelectionEvent
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import kotlin.sequences.Sequence


private const val TITLE = "NetCDF Viewer"


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
            open(event.project!!, path)
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

    private fun getWindow(project: Project) : ToolWindow {
        val manager = ToolWindowManager.getInstance(project)
        var window = manager.getToolWindow(TITLE)
        if (window == null) {
            // First-time tool registration.
            window = manager.registerToolWindow(
                RegisterToolWindowTask(
                    id = TITLE,
                    icon = IconLoader.getIcon("/icons/ic_extension.svg", javaClass),
                    component = null,
                    canCloseContent = false,
                    canWorkInDumbMode = true,
                )
            )
        }
        return window.also { it.setToHideOnEmptyContent(true) }
    }

    private fun open(project: Project, path: String) {
        val window = getWindow(project)
        window.contentManager.removeAllContents(true)
        val fileTab = FileTab(path)
        val dataTab = DataTab(fileTab)
        val treeTab = TreeTab(fileTab)
        listOf(fileTab, dataTab, treeTab).forEach { it.addContent(window) }
        window.show()
        return
    }
}


/**
 * Interface for tool window content tabs.
 */
private interface ToolWindowTab {

    /** Tab display title. */
    val title: String

    /** Tab description. */
    val description: String

    /** Top-level tab component. */
    val component: JComponent

    /**
     * Add this tab to a ToolWindow.
     *
     * @param window: window to add tab to
     * @return: resulting content index in window
     */
    fun addContent(window: ToolWindow): Int {
        val factory = window.contentManager.factory
        factory.createContent(component, title, false).also {
            it.description = description
            it.setDisposer { dispose() }
            window.contentManager.addContent(it)
        }
        return window.contentManager.contentCount - 1
    }

    /**
     * Dispose of tab resources when it is closed.
     */
    fun dispose() {
        return
    }
}


/**
 * Display file schema.
 */
internal class FileTab(path: String) : JBTable(Model()), ToolWindowTab {
    override val title = "Schema"
    override val description = "File schema"
    override val component = JBScrollPane(this)

    override fun dispose() {
        (model as Model).clear()
        file.close()  // TODO: might this happen before other tabs are done with it?
        return
    }

    private val logger = Logger.getInstance(this::class.java)  // runtime class resolution
    internal val file = NetcdfFile.open(path)
    internal var selectedVars = emptyList<String>()

    init {
        logger.info("Opened netCDF file ${file.location}")
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        selectionModel.addListSelectionListener(this::selectionListener)
        (model as Model).setData(file)
        formatColumns()
    }


    /**
     * Handle row selection events.
     */
    private fun selectionListener(event: ListSelectionEvent?) {
        // TODO: Does event need to be validated?
        if (event == null || selectionModel.isSelectionEmpty) {
            return
        }
        val model = this.model as Model
        val dimensions = selectedRows.map { model.getValueAt(it, 2) }.toSet()
        if (dimensions.size > 1) {
            Messages.showMessageDialog(
                component,
                "All selected variables must have the same dimensions",
                TITLE,
                Messages.getErrorIcon()
            )
            val index = selectionModel.anchorSelectionIndex
            selectionModel.removeSelectionInterval(index, index)
            return
        }
        selectedVars = selectedRows.map { model.getValueAt(it, 0) }.toList()
    }

    /**
     * Set column formatting.
     */
    private fun formatColumns() {
        columnModel.columns.asSequence().forEach {
            it.headerRenderer = object : DefaultTableCellRenderer() {
                // Set column labels to bold.
                override fun setFont(font: Font?) {
                    super.setFont(font?.deriveFont(Font.BOLD))
                }
            }
        }
        return
    }

    /**
     * Table model for file schema data.
     */
    class Model : AbstractTableModel() {

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
        internal fun setData(file: NetcdfFile) {
            logger.debug("Loading schema from ${file.location}")
            schema = file.variables.map {
                var dataType = if (it.isArrayString) "char[]" else it.dataType.name.toLowerCase()
                if (it.isTime) {
                    dataType = "time<${dataType}>"
                }
                arrayOf(
                    it.fullNameEscaped,
                    it.description,
                    it.nameAndDimensions.substring(it.nameAndDimensions.lastIndexOf("(")),
                    it.unitsString,
                    dataType,
                )
            }.toList()
            fireTableStructureChanged()
        }

        internal fun clear() {
            schema = emptyList()
            return
        }
    }
}


/**
 * Display variable data.
 */
internal class DataTab(private val fileTab: FileTab) : JBTable(Model()), ToolWindowTab {

    override val title = "Data"
    override val description = "Selected variables"
    override val component = JBScrollPane(this)

    private var displayedVars = emptyList<String>()

    init {
        emptyText.text = "Select variable(s) in Schema tab"
        autoCreateRowSorter = true
    }

    override fun addContent(window: ToolWindow): Int {
        val index = super.addContent(window)
        window.addContentManagerListener(object : ContentManagerListener {
            override fun selectionChanged(event: ContentManagerEvent) {
                // Lazy loading when tab is selected.
                super.selectionChanged(event)
                if (event.operation.name == "add" && event.index == index) {
                    load()
                }
            }
        })
        return index
    }

    override fun dispose() {
        (model as Model).clear()
        return
    }

    /**
     * Load variables from the netCDF file.
     */
    fun load() {
        if (displayedVars == fileTab.selectedVars) {
            return  // selected variables are already displayed
        }
        (model as Model).setData(fileTab.file, fileTab.selectedVars.asSequence())
        displayedVars = fileTab.selectedVars
        formatColumns()
        return
    }


    /**
     * Set column formatting.
     */
    private fun formatColumns() {
        columnModel.columns.asSequence().forEach {
            var headerStyle = Font.BOLD
            var cellStyle = Font.PLAIN
            if (!fileTab.selectedVars.contains(it.headerValue)) {
                // Add italics to coordinate columns.
                headerStyle = headerStyle or Font.ITALIC
                cellStyle = cellStyle or Font.ITALIC
            }
            it.headerRenderer = object : DefaultTableCellRenderer() {
                // Set header style.
                override fun setFont(font: Font?) {
                    super.setFont(font?.deriveFont(headerStyle))
                }
            }
            it.cellRenderer = object : DefaultTableCellRenderer() {
                // Set regular cell style.
                override fun setFont(font: Font?) {
                    super.setFont(font?.deriveFont(cellStyle))
                }
            }
        }
        return
    }

    class Model : AbstractTableModel() {

        private val logger = Logger.getInstance(this::class.java)  // runtime class resolution
        var table: TableView? = null

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
        override fun getColumnClass(columnIndex: Int): Class<*> =
            table?.column(columnIndex)?.type ?: throw IllegalStateException("empty table")

        /**
         * Get the name label for a column.
         *
         */
        override fun getColumnName(column: Int) =
            table?.column(column)?.label ?: throw IllegalStateException("empty table")

        /**
         * Returns the value for the cell at `columnIndex` and
         * `rowIndex`.
         *
         * @param   rowIndex        the row whose value is to be queried
         * @param   columnIndex     the column whose value is to be queried
         * @return  the value Object at the specified cell
         */
        override fun getValueAt(rowIndex: Int, columnIndex: Int) =
            table?.column(columnIndex)?.value(rowIndex) ?: throw IllegalStateException("empty table")

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
            clear()  // TODO: is this necessary?
            table = TableView(file).also { it.add(varNames) }
            fireTableStructureChanged()
            return
        }

        internal fun clear() {
            table?.clear()
        }
    }
}

/**
 * File schema tree view (experimental).
 */
internal class TreeTab(private val fileTab: FileTab) : Tree(), ToolWindowTab {

    override val title = "Tree"
    override val description = "File schema (experimental tree view)"
    override val component = JBScrollPane(this)

    private var root: DefaultMutableTreeNode? = null
    private var view = TreeView(fileTab.file)

    fun load() {
        root = DefaultMutableTreeNode(fileTab.file.location).also {
            model = DefaultTreeModel(it)
            addGroup(it, view.root)
            expandPath(TreePath(it))
        }
        return
    }

    override fun addContent(window: ToolWindow): Int {
        val index = super.addContent(window)
        window.addContentManagerListener(object : ContentManagerListener {
            override fun selectionChanged(event: ContentManagerEvent) {
                // Lazy loading when tab is selected.
                super.selectionChanged(event)
                if (event.operation.name == "add" && event.index == index) {
                    load()
                }
            }
        })
        return index
    }

    override fun dispose() {
        root?.removeAllChildren()
    }

    private fun addGroup(head: DefaultMutableTreeNode, group: TreeView.Group) {
        val node: DefaultMutableTreeNode
        if (group.isRoot) {
            node = head
        } else {
            node = DefaultMutableTreeNode(group.label)
            head.add(node)
        }
        group.attributes.forEach { node.add(DefaultMutableTreeNode(it)) }
        addVariables(node, group.variables)
        if (group.groups.count() > 0) {
            DefaultMutableTreeNode("Groups").let {
                node.add(it)
                group.groups.forEach { sub -> addGroup(it, sub) }
            }
        }
        return
    }

    private fun addVariables(head: DefaultMutableTreeNode, items: Sequence<TreeView.Variable>) {
        if (items.count() == 0) {
            return
        }
        DefaultMutableTreeNode("Variables").let { node ->
            head.add(node)
            items.forEach {
                DefaultMutableTreeNode(it.label).apply {
                    node.add(this)
                    it.attributes.forEach { this.add(DefaultMutableTreeNode(it)) }
                    addDimensions(this, it.dimensions)
                }
            }
        }
        return
    }

    private fun addDimensions(head: DefaultMutableTreeNode, items: Sequence<String>) {
        if (items.count() == 0) {
            return
        }
        DefaultMutableTreeNode("Dimensions").let { node ->
            head.add(node)
            items.forEach { node.add(DefaultMutableTreeNode(it)) }
        }
        return
    }
}
