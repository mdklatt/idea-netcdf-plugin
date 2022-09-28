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
import com.intellij.ui.table.JBTable
import com.intellij.ui.treeStructure.Tree
import dev.mdklatt.idea.netcdf.files.NetcdfFileType
import ucar.nc2.NetcdfFile
import ucar.nc2.NetcdfFiles
import java.awt.Font
import javax.swing.JComponent
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.event.TreeSelectionEvent
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.tree.*


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
            val schemaTab = SchemaTab(ncPath)
            val dataTab = DataTab(schemaTab)
            ToolWindowManager.getInstance(project).getToolWindow(TITLE)?.let {
                // This is kind of a mess, but it's working. The IoC by using
                // ViewTab.addContent() seems like a code smell in this case.
                // TODO: The JBTabbedPane component needs to be a class, e.g. FileView.
                it.setToHideOnEmptyContent(true)
                it.contentManager.let { cm ->
                    val pane = JBTabbedPane()
                    sequenceOf(schemaTab, dataTab).forEach { tab ->
                        tab.addContent(pane)
                    }
                    cm.factory.createContent(pane, ncPath, false).let { content ->
                        // TODO: Use file name only for title
                        content.description = ncPath
                        cm.addContent(content)
                        content.setDisposer {
                            // FIXME: Two layers deep, definitely a code smell.
                            sequenceOf(schemaTab, dataTab).forEach { tab ->
                                tab.dispose()
                            }
                        }
                    }
                }
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
        // TODO: Allow drag and drop to open new window?
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
     * Add this tab to a JTabbedPane.
     *
     * @param parent: parent component
     * @return: index of this tab in the parent component
     */
    fun addContent(parent: JBTabbedPane): Int {
        parent.addTab(title, null, component, description)
        return parent.tabCount - 1  // possible race condition?
    }

    /**
     * Dispose of tab resources when it is closed.
     */
    fun dispose()
}


/**
 * File schema tree view.
 */
internal class SchemaTab(path: String) : Tree(), ViewerTab {

    override val title = "Schema"
    override val description = "File schema"
    override val component = JBScrollPane(this)

    private val logger = Logger.getInstance(this::class.java)  // runtime class resolution
    val file: NetcdfFile = NetcdfFiles.open(path)
    var selectedVars = emptyList<String>()

    init {
        logger.debug("Loading s${file.location} in viewer")
        this.model = FileModel().also {
            it.fillTree(file)
            expandPath(TreePath(it.root))
        }
        setSelectionModel(SelectionModel())
        addTreeSelectionListener(this::selectionListener)
        setCellRenderer(DefaultTreeCellRenderer())  // enhanced styling
    }

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
                "Selected variables have different dimensions",
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
         * Set the selection path if the last component is a Variable node.
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
         * Add to the selection path if the last component is a Variable node.
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
internal class DataTab(private val schemaTab: SchemaTab) : JBTable(DataModel()), ViewerTab {

    override val title = "Data"
    override val description = "Data table for selected variables"
    override val component = JBScrollPane(this)

    private var displayedVars = emptyList<String>()

    init {
        emptyText.text = "Select variable(s) in File tab"
        autoCreateRowSorter = true
    }

    override fun addContent(parent: JBTabbedPane): Int {
        val index = super.addContent(parent)
        parent.addChangeListener(object: ChangeListener {
            /**
             * Invoked when parent state chages..
             *
             * @param event  a ChangeEvent object
             */
            override fun stateChanged(event: ChangeEvent?) {
                // Lazy loading when this tab is selected.
                if ((event?.source as JBTabbedPane).selectedIndex == index) {
                    load()
                }
                return
            }
        })
        return index
    }

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
