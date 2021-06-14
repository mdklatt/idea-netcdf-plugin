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
import javax.swing.JOptionPane
import javax.swing.ListSelectionModel
import javax.swing.table.DefaultTableModel


/**
 * Display the file schema.
 */
internal class SchemaTab(
    private var ncfile: NetcdfFile?,
    private var dataTab: DataTab
) : JBTable(DefaultTableModel()), DumbAware {

    private val logger = Logger.getInstance(this::class.java)  // runtime class resolution

    init {
        emptyText.text = "Drop netCDF file here to open"
        dropTarget = createDropTarget()
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        addSelectionListener()
    }

    /**
     *
     */
    private fun readFile() {
        val model = this.model as DefaultTableModel
        model.setDataVector(emptyArray(), emptyArray())
        model.setColumnIdentifiers(arrayOf("Variable", "Description", "Units", "Type"))
        ncfile!!.variables.forEach {
            model.addRow(arrayOf(
                it.nameAndDimensions,
                it.description ?: "",
                it.unitsString ?: "",
                it.dataType.name,
            ))
        }
        return
    }

    private fun createDropTarget(): DropTarget {
        return object : DropTarget() {
            @Synchronized
            override fun drop(event: DropTargetDropEvent) {
                try {
                    event.acceptDrop(DnDConstants.ACTION_COPY)
                    val accepted = event.transferable.getTransferData(DataFlavor.javaFileListFlavor)
                    val file = (accepted as? List<*>)?.get(0) as File
                    ncfile = NetcdfFile.open(file.path)  // TODO: does this throw or return null on failure?
                    logger.info("Opening netCDF file ${ncfile!!.location}")
                    readFile()
                } catch (e: UnsupportedFlavorException) {
                    JOptionPane.showMessageDialog(null, "Unable to read file")
                } catch (e: IOException) {
                    JOptionPane.showMessageDialog(null, "Unable to read file")
                }
                return
            }
        }
    }

    fun addSelectionListener() {
        selectionModel.addListSelectionListener { event ->
            val variables = selectedRows.map { ncfile!!.variables[it] }.toTypedArray()
            dataTab.readFile(ncfile!!, variables)
        }
    }
}


/**
 * Display file data.
 */
internal class DataTab() : JBTable(DefaultTableModel()), DumbAware {

    private val logger = Logger.getInstance(this::class.java)  // runtime class resolution

    /**
     * Read data from selected variables.
     *
     * All variables must have the same dimensions.
     *
     * @param ncfile: open netCDF file
     * @param variables: variables to display
     */
    fun readFile(ncfile: NetcdfFile, variables: Array<Variable>) {
        val columnLabels = arrayListOf<String>()
        var dimensions: List<Dimension>? = null
        variables.forEach {
            // Make sure all dimensions match.
            if (dimensions == null) {
                dimensions = it.dimensions
            } else if (dimensions != it.dimensions) {
                throw RuntimeException("all variables must have the same dimensions")
            }
            columnLabels.add(it.fullName)
        }
        columnLabels.add(0, "(${dimensions!!.joinToString(", ") { it.fullName }})")
        val model = this.model as DefaultTableModel
        model.setDataVector(arrayOf(), arrayOf())
        model.setColumnIdentifiers(columnLabels.toArray())
        val indexCoords = arrayListOf<List<Int>>()
        val indexValues = arrayListOf<List<String>>()
        dimensions!!.forEach {
            val values = readDimension(ncfile, it)
            indexCoords.add((0..values.lastIndex).toList())
            indexValues.add(values)
        }
        val coords = cartProd(*(indexCoords.toTypedArray()))
        val labels = cartProd(*(indexValues.toTypedArray())).map {
            "(${it.joinToString(", ")})"
        }
        for ((index, item) in coords.zip(labels).withIndex()) {
            val row = mutableListOf<Any>(item.second)
            for (variable in variables) {
                val shape = IntArray(variable.rank) { 1 }
                row.add(variable.read(item.first.toIntArray(), shape))
            }
            model.addRow(row.toTypedArray())
            if (index == 9) {
                break
            }
        }
        return
    }

    /**
     * Get dimension values.
     *
     * If there is a dimension variable for this dimension, the variable values
     * are returned. Otherwise, use the variable indexes.
     *
     * @param file: open netCDF file
     * @param dimension: netCDF dimension
     * @return: dimension values
     */
    private fun readDimension(file: NetcdfFile, dimension: Dimension): List<String> {
        logger.debug("Reading dimension ${dimension.fullName}")
        val dimensionVar = file.findVariable(dimension.fullName)
        val values = mutableListOf<String>()
        if (dimensionVar != null) {
            if (dimensionVar.rank > 1) {
                throw RuntimeException("dimension variable does not have rank of 1")
            }
            dimensionVar.read().indexIterator.apply {
                while (hasNext()) {
                    values.add(objectNext.toString())
                }
            }
        } else {
            val last = file.findDimension(name).length - 1  // inclusive
            values.addAll((0..last).map { it.toString() })
        }
        return values.toList()
    }
}


/**
 * NetCDF viewer display.
 */
class NetcdfToolWindow: ToolWindowFactory, DumbAware {

    private var netcdfFile: NetcdfFile? = null

    /**
     * Create tool window content.
     *
     * @param project: current project
     * @param window current tool window
     */
    override fun createToolWindowContent(project: Project, window: ToolWindow) {
        val factory = window.contentManager.factory
        val dataTab = DataTab()
        val schemaTab = SchemaTab(netcdfFile, dataTab)
        factory.createContent(JBScrollPane(schemaTab), "Schema", false).let {
            window.contentManager.addContent(it)
        }
        factory.createContent(JBScrollPane(dataTab), "Data", false).let {
            window.contentManager.addContent(it)
        }
        return
    }
}
