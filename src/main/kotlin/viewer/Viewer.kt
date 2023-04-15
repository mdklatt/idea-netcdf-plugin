/**
 * Implementation of the NetCDF Viewer tool.
 */
package dev.mdklatt.idea.netcdf.viewer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentManager
import com.intellij.ui.table.JBTable
import com.intellij.ui.treeStructure.Tree
import dev.mdklatt.idea.netcdf.files.NetcdfFileType
import java.awt.Font
import javax.swing.JComponent
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.event.TreeSelectionEvent
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.tree.*
import kotlin.io.path.*
import ucar.nc2.NetcdfFile
import ucar.nc2.NetcdfFiles
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel


private const val TITLE = "NetCDF"  // must match toolWindow ID in plugin.xml


class ViewerWindowFactory : ToolWindowFactory {

    companion object {
        /**
         * Add content to existing tool window.
         *
         * @param project current project reference
         * @param ncPath netCDF file path
         */
        internal fun addContent(project: Project, ncPath: String) {
            ToolWindowManager.getInstance(project).getToolWindow(TITLE)?.let {
                FilePane(ncPath).createContent(it.contentManager)
                it.setToHideOnEmptyContent(true)
                it.show()
            } ?: throw RuntimeException("Could not get $TITLE tool window")
        }
    }

    /**
     * Called by IDE to create new tool window.
     */
    override fun createToolWindowContent(project: Project, window: ToolWindow) {
        // Nothing to do show until user selects a netCDF file.
        // TODO: Display message, e.g. "Select netCDF file in Project window".
        // TODO: Allow drag and drop to open new file?
    }
}


/**
 * Handler for the "NetCDF > Open in Viewer" action.
 */
class OpenViewerAction : AnAction() {
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
            // project reference is never null.
            ViewerWindowFactory.addContent(event.project!!, path)
        } else {
            Messages.showMessageDialog(
                event.project,
                "Not a netCDF file",
                event.presentation.text,
                Messages.getErrorIcon()
            )
        }
    }
}


/**
 * Root element for displaying a netCDF file.
 *
 * Each file will have a file schema tab and a data view tab.
 */
internal class FilePane(private val ncPath: String): JBTabbedPane() {

    private val schemaTab = SchemaTab(ncPath)
    private val dataTab = DataTab(schemaTab)

    private val tabs: Sequence<FileTab>
        get() = sequenceOf(schemaTab, dataTab)

    init {
        tabs.forEach { it.addContent(this) }
    }

    fun dispose() {
        tabs.forEach { it.dispose() }
    }

    fun createContent(contentManager: ContentManager) {
        val name = Path(ncPath).name
        contentManager.factory.createContent(this, name, false).let {
            it.description = ncPath
            it.setDisposer(this::dispose)
            contentManager.addContent(it)
        }
    }
}


/**
 * Interface for FilePane content tabs.
 */
private interface FileTab {

    /** Tab display title. */
    val title: String

    /** Tab description. */
    val description: String

    /** Top-level tab component. */
    val component: JComponent

    /**
     * Add this tab to a JTabbedPane.
     *
     * @param parent: parent component
     * @return: index of this tab in the parent component
     */
    fun addContent(parent: FilePane): Int {
        parent.addTab(title, null, component, description)
        return parent.tabCount - 1  // possible race condition?
    }

    /**
     * Dispose of tab resources on close.
     */
    fun dispose()
}


/**
 * File schema tree view.
 */
internal class SchemaTab(ncPath: String) : Tree(), FileTab {

    override val title = "Schema"
    override val description = "File schema"
    override val component = JBScrollPane(this)

    private val logger = Logger.getInstance(this::class.java)  // runtime class resolution
    val file: NetcdfFile = NetcdfFiles.open(ncPath)
    var selectedVars = emptyList<String>()

    init {
        logger.debug("Loading ${file.location} in viewer")
        this.model = FileModel().also {
            it.fillTree(file)
            expandPath(TreePath(it.root))
        }
        setSelectionModel(SelectionModel())
        addTreeSelectionListener(this::selectionListener)
        setCellRenderer(DefaultTreeCellRenderer())  // enhanced styling
    }

    /**
     * Dispose of resources for this element.
     */
    override fun dispose() {
        (model as FileModel).clearTree()
        file.close()
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
            it.userObject as? FileModel.Variable
        }
        if (variables.map { it.dimensions.joinToString(",") }.toSet().size > 1) {
            Messages.showMessageDialog(
                component,
                "All variables in the selection must have compatible dimensions",
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

        init {
            selectionMode = DISCONTIGUOUS_TREE_SELECTION
        }

        /**
         * Restrict the selection path to Variable nodes.
         *
         * @param path: selection path
         */
        override fun setSelectionPath(path: TreePath?) {
            val node = path?.lastPathComponent as DefaultMutableTreeNode
            if (node.userObject is FileModel.Variable) {
                super.setSelectionPath(path)
            }
        }

        /**
         * Add Variable nodes only to the selection path.
         *
         * @param path: selection path
         */
        override fun addSelectionPath(path: TreePath?) {
            val node = path?.lastPathComponent as DefaultMutableTreeNode
            if (node.userObject is FileModel.Variable) {
                super.addSelectionPath(path)
            }
        }
    }
}


/**
 * Variable data table view.
 */
internal class DataTab(private val schemaTab: SchemaTab) : JBTable(DataModel()), FileTab {

    override val title = "Data"
    override val description = "Data table for selected variables"
    override val component = JPanel()

    private var displayedVars = emptyList<String>()
    private val pager = Pager(model as DataModel)

    /**
     * Page selector component.
     */
    private class Pager(private val model: DataModel): JPanel() {

        private val counter = JLabel()

        /**
         * Draw components.
         */
        fun draw() {
            removeAll()
            listOf(
                Triple("<<<", -model.pageCount, "First page"),
                Triple("<<", -10, "Back 10 pages"),
                Triple("<", -1, "Back 1 page"),
                Triple(">", 1, "Forward 1 page"),
                Triple(">>", 10, "Forward 10 pages"),
                Triple(">>>", model.pageCount, "Last page"),
            ).forEach { (text, increment, description) ->
                addButton(text, increment, description)
            }
            add(counter)
            updateCounter()
        }

        /**
         * Update the counter.
         */
        private fun updateCounter() {
            counter.text = "${model.pageNumber} / ${model.pageCount}"
        }

        /**
         * Add a button.
         */
        private fun addButton(text: String, increment: Int, description: String) {
            add(JButton(text).also {
                it.toolTipText = description
                it.addActionListener {
                    model.pageNumber += increment
                    updateCounter()
                }
            })
        }
    }

    init {
        emptyText.text = "Select variable(s) in File tab"
        autoCreateRowSorter = true
        component.let {
            it.layout = BorderLayout()
            it.add(JBScrollPane(this), BorderLayout.CENTER)
            it.add(pager, BorderLayout.PAGE_END)
        }
    }

    /**
     * Add this tab to a FilePane.
     */
    override fun addContent(parent: FilePane): Int {
        // Override parent to add event listener.
        val index = super.addContent(parent)
        parent.addChangeListener(object: ChangeListener {
            /**
             * Invoked when parent state changes.
             *
             * @param event  a ChangeEvent object
             */
            override fun stateChanged(event: ChangeEvent?) {
                // Lazy loading when this tab is selected.
                if ((event?.source as JBTabbedPane).selectedIndex == index) {
                    load()
                }
            }
        })
        return index
    }

    /**
     * Dispose of resources for this element.
     */
    override fun dispose() {
        (model as DataModel).clearTable()
    }

    /**
     * Load variables from the netCDF file.
     */
    fun load() {
        if (displayedVars == schemaTab.selectedVars) {
            return  // selected variables are already displayed
        }
        (model as DataModel).fillTable(schemaTab.file, schemaTab.selectedVars.asSequence())
        displayedVars = schemaTab.selectedVars
        formatColumns()
        pager.draw()
    }

    /**
     * Set column formatting.
     */
    private fun formatColumns() {
        columnModel.columns.asSequence().forEach {
            var headerStyle = Font.BOLD
            var cellStyle = Font.PLAIN
            if (!schemaTab.selectedVars.contains(it.headerValue)) {
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
    }
}
