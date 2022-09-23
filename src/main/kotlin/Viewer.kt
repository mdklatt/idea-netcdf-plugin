/**
 * Implementation of the NetCDF Viewer tool.
 */
package dev.mdklatt.idea.netcdf

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
import dev.mdklatt.idea.netcdf.files.NetcdfFileType
import ucar.nc2.NetcdfFile
import java.awt.Font
import javax.swing.JComponent
import javax.swing.event.TreeSelectionEvent
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.DefaultTreeSelectionModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
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
            // TODO: Deprecated: "Use ToolWindowFactory and toolWindow extension point"
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
        //val fileTab = FileTab(path)
        val fileTab = FileTab(path)
        val dataTab = DataTab(fileTab)
        listOf(fileTab, dataTab).forEach { it.addContent(window) }
        window.show()
        return
    }
}

// TODO: Display File and Data view as tabs in a single tab, and use tabs for multiple files.


/**
 * Interface for tool window content tabs.
 */
private interface ViewerTab {

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
 * File schema tree view.
 */
internal class FileTab(path: String) : Tree(), ViewerTab {

    override val title = "File"
    override val description = "File schema (experimental tree view)"
    override val component = JBScrollPane(this)

    private val logger = Logger.getInstance(this::class.java)  // runtime class resolution
    val file: NetcdfFile = NetcdfFile.open(path)
    var selectedVars = emptyList<String>()

    private var view = FileView(file)
    private var root = DefaultMutableTreeNode(file.location).also {
        model = DefaultTreeModel(it)
        addGroup(it, view.root)
        expandPath(TreePath(it))
    }
    init {
        logger.debug("Loading schema from ${file.location}")
        selectionModel = SelectionModel()
        selectionModel.selectionMode = DISCONTIGUOUS_TREE_SELECTION
        addTreeSelectionListener(this::selectionListener)
    }

    override fun dispose() {
        root.removeAllChildren()
        file.close()
    }

    private fun addGroup(head: DefaultMutableTreeNode, group: FileView.Group) {
        val node: DefaultMutableTreeNode
        if (group.isRoot) {
            node = head
        } else {
            node = DefaultMutableTreeNode(group)
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

    private fun addVariables(head: DefaultMutableTreeNode, items: Sequence<FileView.Variable>) {
        if (items.count() == 0) {
            return
        }
        DefaultMutableTreeNode("Variables").let { node ->
            head.add(node)
            items.forEach {
                DefaultMutableTreeNode(it).apply {
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

    /**
     * Handle node selection events.
     */
    private fun selectionListener(event: TreeSelectionEvent?) {
        // TODO: Does event need to be validated?
        if (event == null || selectionModel.isSelectionEmpty) {
            return
        }
        val variables = getSelectedNodes(DefaultMutableTreeNode::class.java, null).mapNotNull {
            it.userObject as? FileView.Variable
        }
        if (variables.map { it.dimensions.joinToString(",") }.toSet().size > 1) {
            Messages.showMessageDialog(
                component,
                "All selected variables must have compatible dimensions",
                TITLE,
                Messages.getErrorIcon()
            )
            removeSelectionPath(selectionModel.selectionPaths.last())
        } else {
            selectedVars = variables.map { it.name }
        }
        return
    }

    /**
     * Restrict selections to Variable nodes.
     */
    private class SelectionModel : DefaultTreeSelectionModel() {
        /**
         * Set the selection path if the last component is a Variable node.
         *
         * @param path: selection path
         */
        override fun setSelectionPath(path: TreePath?) {
            val node = path?.lastPathComponent as DefaultMutableTreeNode
            if (node.userObject is FileView.Variable) {
                super.setSelectionPath(path)
            }
            return
        }

        /**
         * Add to the selection path if the last component is a Variable node.
         *
         * @param path: selection path
         */
        override fun addSelectionPath(path: TreePath?) {
            val node = path?.lastPathComponent as DefaultMutableTreeNode
            if (node.userObject is FileView.Variable) {
                super.addSelectionPath(path)
            }
            return
        }
    }
}


/**
 * Display variable data.
 */
internal class DataTab(private val fileTab: FileTab) : JBTable(Model()), ViewerTab {

    override val title = "Data"
    override val description = "Selected variables"
    override val component = JBScrollPane(this)

    private var displayedVars = emptyList<String>()

    init {
        emptyText.text = "Select variable(s) in File tab"
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

    /**
     * Data model for rendering a DataView table.
     */
    class Model : AbstractTableModel() {

        private val logger = Logger.getInstance(this::class.java)
        var view: DataView? = null

        /**
         * Returns the number of rows in the model. A
         * `JTable` uses this method to determine how many rows it
         * should display.  This method should be quick, as it
         * is called frequently during rendering.
         *
         * @return the number of rows in the model
         * @see .getColumnCount
         */
        override fun getRowCount() = view?.rowCount ?: 0

        /**
         * Returns the number of columns in the model. A
         * `JTable` uses this method to determine how many columns it
         * should create and display by default.
         *
         * @return the number of columns in the model
         * @see .getRowCount
         */
        override fun getColumnCount() = view?.columnCount ?: 0

        /**
         *
         */
        override fun getColumnClass(columnIndex: Int): Class<*> =
            view?.column(columnIndex)?.type ?: throw IllegalStateException("empty table")

        /**
         * Get the name label for a column.
         *
         */
        override fun getColumnName(column: Int) =
            view?.column(column)?.label ?: throw IllegalStateException("empty table")

        /**
         * Returns the value for the cell at `columnIndex` and
         * `rowIndex`.
         *
         * @param   rowIndex        the row whose value is to be queried
         * @param   columnIndex     the column whose value is to be queried
         * @return  the value Object at the specified cell
         */
        override fun getValueAt(rowIndex: Int, columnIndex: Int) =
            view?.column(columnIndex)?.value(rowIndex) ?: throw IllegalStateException("empty table")

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
            view = DataView(file).also { it.add(varNames) }
            fireTableStructureChanged()
            return
        }

        fun clear() {
            view?.clear()
        }
    }
}
