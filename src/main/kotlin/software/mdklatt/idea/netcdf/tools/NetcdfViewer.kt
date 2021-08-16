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
import ucar.nc2.Dimension
import ucar.nc2.NetcdfFile
import ucar.nc2.Variable
import ucar.nc2.time.Calendar
import ucar.nc2.time.CalendarDateUnit
import vendor.tandrial.itertools.cartProd
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


/**
 * NetCDF viewer display.
 */
class NetcdfToolWindow: ToolWindowFactory, DumbAware {

    /**
     * File schema content.
     */
    inner class SchemaTab : JBTable(SchemaTableModel()) {
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
        private fun load() {
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
    inner class DataTab : JBTable(DataTableModel()) {
        init {
            emptyText.text = "Select variable(s) in Schema tab"
            autoCreateRowSorter = true
        }

        /**
         * Load variables from the netCDF file.
         */
        internal fun load() {
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

    private var ncFile: NetcdfFile? = null
    private var schemaTab = SchemaTab()
    private var dataTab = DataTab()
    private var selectedVars = emptyList<String>()
    private var displayedVars = emptyList<String>()


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
        window.contentManager
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
    private var indexes = emptyList<IntArray>()
    private var columns = mutableListOf<Column>()
    private var labels = emptyList<String>()

    /**
     * Returns the number of rows in the model. A
     * `JTable` uses this method to determine how many rows it
     * should display.  This method should be quick, as it
     * is called frequently during rendering.
     *
     * @return the number of rows in the model
     * @see .getColumnCount
     */
    override fun getRowCount() = indexes.size

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
    override fun getColumnClass(columnIndex: Int): Class<*> = columns[columnIndex].type

    /**
     * Get the name label for a column.
     *
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
    override fun getValueAt(rowIndex: Int, columnIndex: Int) = columns[columnIndex].read(indexes[rowIndex])

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
        val variables = varNames.map {
            file.findVariable(it) ?: throw IllegalArgumentException("Unknown variable: '${it}'")
        }
        val dimensions = variables.firstOrNull()?.let {
            // If this is a fixed-length array string variable, ignore the last
            // dimension that defines the string length.
            if (!it.isArrayString) it.dimensions else it.dimensions.dropLast(1)
        }?: emptyList()
        val axes = dimensions.map { (0 until it.length) }.toTypedArray()
        columns = dimensions.mapIndexed { axis, it -> createCoordinateColumn(file, it, axis) }.toMutableList()
        columns.addAll(variables.map { createVariableColumn(it, dimensions.indices.toList().toIntArray()) })
        labels = columns.map { it.label }
        indexes = if (axes.isEmpty()) emptyList() else cartProd(*axes).map { it.toIntArray() }.toList()
        fireTableStructureChanged()
        return
    }

    private fun createCoordinateColumn(file: NetcdfFile, dimension: Dimension, axis: Int) : Column {
        val axes = intArrayOf(axis)
        val variable = file.findVariable(dimension.fullNameEscaped)
        return if (variable?.isCoordinateVariable != true) {
            object : Column(dimension.fullNameEscaped) {
                override val type = Long::class.java
                override fun read(index: IntArray) = index[axis]
            }
        } else if (variable.isTime) {
            TimeColumn(variable, axes)
        } else if (variable.isArrayString) {
            ArrayStringColumn(variable, axes)
        } else {
            DataColumn(variable, axes)
        }
    }

    private fun createVariableColumn(variable: Variable, axes: IntArray) : Column {
        return if (variable.isTime) {
            TimeColumn(variable, axes)
        } else if (variable.isArrayString) {
            ArrayStringColumn(variable, axes)
        } else {
            DataColumn(variable, axes)
        }
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


/**
 * True if variable appears to be a character array string.
 *
 * Prior to netCDF4, strings had to be stored as a 2D CHAR array where the
 * second dimension extends along the length of each string.
 *
 * @see <a href=http://www.bic.mni.mcgill.ca/users/sean/Docs/netcdf/guide.txn_58.html>Reading and Writing Character String Values</a>
 */
private val Variable.isArrayString: Boolean
    get() = dataType.name.toLowerCase() == "char" && shape.size == 2


/**
 * True if variable appears to be a time variable.
 *
 * This variable is assumed to contain time values if it adheres to netCDF time
 * conventions, namely that it is a numeric variable whose name starts with
 * 'time' and has a 'units' attribute of the form '<units> since <timestamp>'
 *
 * @see <a href="https://www.unidata.ucar.edu/software/netcdf/time/recs.html">A Brief History of (netCDF) Time<a>
 */
private val Variable.isTime : Boolean
    get() {
        val name = fullNameEscaped.split("/").last()
        val regex = CalendarDateUnit.udunitPatternString.toRegex()
        return name.startsWith("time", 0) && dataType.isNumeric && regex.matches(unitsString.toLowerCase())
    }


/**
 * Define a table column.
 */
internal abstract class Column(val label: String) {

    /**
     * Data type represented by this column.
     */
    open val type : Class<*> = Any::class.java

    /**
     * Read a column value for the given index.
     *
     * @param index:
     * @return: column value
     */
    abstract fun read(index: IntArray) : Any
}


/**
 * Table column holding a netCDF variable.
 *
 * The `axes` parameter maps table dimensions to variable dimensions, e.g. if
 * the table dataspace has dimensions ("lat", "lon"), and the variable has
 * dimensions ("lon", "lat"), the `axes` value should be [1, 0].
 *
 * @param variable: netCDF variable
 * @param axes: map table dimensions to variable dimensions
 */
internal open class DataColumn(private val variable: Variable, private var axes: IntArray) : Column(variable.fullNameEscaped) {

    /**
     * Data type represented by this column.
     */
    override val type: Class<*> = variable.dataType.primitiveClassType

    /**
     * Shape required to read a single variable element.
     */
    protected open val shape = IntArray(variable.dimensions.size) { 1 }

    /**
     * Translate an index to an origin point for this variable. This is used in
     * conjunction with `shape` to define the array section required to read a
     * single value from the variable.
     */
    protected open fun origin(index: IntArray) = axes.map { index[it] }.toIntArray()

    /**
     * Read value(s) from the underlying netCDF variable. Child classes should
     * override this if single column value is composed of multiple variable
     * values.
     *
     * @param index: value index in the table dataspace
     * @return: variables values(s)
     */
    protected fun readVariable(index: IntArray) : ucar.ma2.Array = variable.read(origin(index), shape)


    /**
     * Get a single column value for the given index.
     *
     * @param index: value index in the table dataspace
     * @return: column value
     */
    override fun read(index: IntArray) : Any = readVariable(index).getObject(0)
}


/**
 * Column of netCDF time values.
 *
 * @see <a href="https://www.unidata.ucar.edu/software/netcdf/time/recs.html">A Brief History of (netCDF) Time<a>
 */
internal class TimeColumn(variable: Variable, axes: IntArray) : DataColumn(variable, axes) {

    private val calendar = variable.findAttribute("calendar")?.stringValue ?: Calendar.getDefault().name
    private val units = CalendarDateUnit.of(calendar, variable.unitsString)

    /** @see DataColumn.type */
    override val type = String::class.java

    /** @see DataColumn.read */
    override fun read(index: IntArray) : Any {
        // Override base class to convert numeric time offsets to ISO 8601
        // timestamps.
        val dataValue = readVariable(index)
        val timeValue = if (dataValue.dataType.isIntegral) {
            units.makeCalendarDate(dataValue.getObject(0).toString().toInt())
        } else {
            units.makeCalendarDate(dataValue.getObject(0).toString().toDouble())
        }
        return timeValue.toString()
    }
}


/**
 * Column of fixed-length "classic" strings.
 *
 * @see <a href=http://www.bic.mni.mcgill.ca/users/sean/Docs/netcdf/guide.txn_58.html>Reading and Writing Character String Values</a>
 */
internal class ArrayStringColumn(variable: Variable, axes: IntArray) : DataColumn(variable, axes) {

    private val strLength = variable.shape.last()

    /** @see DataColumn.type */
    override val type = String::class.java

    /** @see DataColumn.shape */
    // Override base class to account for additional string length dimension.
    override val shape = IntArray(variable.shape.size - 1) { 1 } + intArrayOf(strLength)

    /** @see DataColumn.origin */
    // Override base class to account for additional string length dimension.
    override fun origin(index: IntArray) = super.origin(index) + intArrayOf(0)

    /** @see DataColumn.read */
    // Override base class to convert a character array to a String.
    override fun read(index: IntArray) = readVariable(index).toString()
}
