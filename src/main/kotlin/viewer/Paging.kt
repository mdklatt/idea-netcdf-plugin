package dev.mdklatt.idea.netcdf.viewer

import com.intellij.ui.components.JBTextField
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.table.AbstractTableModel
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.max


/**
 * Represent tabular data as multiple pages. Only data for the current page
 * will be selectable.
 */
abstract class PageableTableModel: AbstractTableModel() {

    var pageCount: Int = 0
        protected set

    var pageSize = 50
        set(value) {
            field = value
            updatePageCount()
        }

    var pageNumber: Int = 0
        set(value) {
            field = if (pageCount == 0) 0 else value.coerceIn(1, pageCount)
            fireTableDataChanged()
        }

    protected var dataRowCount = 0
        set(value) {
            field = max(value, 0)
            updatePageCount()
        }

    protected val pageRowCount
        get() =
            if (pageNumber == pageCount) {
                dataRowCount - ((pageCount - 1) * pageSize)
            } else {
                pageSize
            }

    /**
     * Update the number of pages.
     */
    private fun updatePageCount() {
        // For performance, only do this calculation as needed rather than
        // overriding pageCount.get().
        val numerator = dataRowCount.toDouble()
        pageCount = ceil(numerator.div(pageSize)).toInt()
    }

    /**
     * Translate a page row index to corresponding data row.
     */
    protected fun dataRowIndex(pageRowIndex: Int) = (pageNumber - 1) * pageSize + pageRowIndex

    /**
     * Returns the number of rows in the model. A
     * `JTable` uses this method to determine how many rows it
     * should display.  This method should be quick, as it
     * is called frequently during rendering.
     *
     * @return the number of rows in the model
     * @see .getColumnCount
     */
    override fun getRowCount() = pageRowCount
}


/**
 * Page selector UI component.
 */
internal class Pager(private val model: PageableTableModel): JPanel() {

    private val pageNumber = JBTextField().also { field ->
        field.addActionListener {
            updatePageNumber(field.text.toInt())
        }
    }

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
        val maxLength = ceil(log10(model.pageCount.toDouble())).toInt()
        pageNumber.columns = max(maxLength, 1) + 1  // extra column for padding
        add(JLabel("Page: "))
        add(pageNumber)
        add(JLabel(model.pageCount.toString()))
        updatePageNumber(model.pageNumber)
    }

    /**
     * Update the page number.
     *
     * @param number: new page number (first page is 1)
     */
    private fun updatePageNumber(number: Int) {
        // Round-trip the value to `pages` so it can validate it.
        model.pageNumber = number
        pageNumber.text = model.pageNumber.toString()
    }

    /**
     * Add a button.
     */
    private fun addButton(text: String, increment: Int, description: String) {
        add(JButton(text).also {
            it.toolTipText = description
            it.addActionListener {
                updatePageNumber(model.pageNumber + increment)
            }
        })
    }
}
