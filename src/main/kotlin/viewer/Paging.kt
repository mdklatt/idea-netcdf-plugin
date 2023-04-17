package dev.mdklatt.idea.netcdf.viewer

import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.math.ceil


/**
 * Interface for data that can be paged.
 */
internal interface Pageable {

    /**
     * Get the total number of rows to be paged.
     *
     * @return row count
     */
    fun getTotalRowCount(): Int

    /**
     * Get the number of rows per page.
     *
     * @return row count
     */
    fun getPageSize(): Int

    /**
     * Get the total number of pages.
     *
     * @return page count
     */
    fun getPageCount(): Int {
        val numerator = getTotalRowCount().toDouble()
        return ceil(numerator.div(getPageSize())).toInt()
    }

    /**
     * Get the current page number.
     *
     * @return page number (first page is 1)
     */
    fun getPageNumber(): Int

    /**
     * Set the current page number
     *
     * @param value page number (first page is 1)
     */
    fun setPageNumber(value: Int)
}


/**
 * Page selector UI component.
 */
internal class Pager(private val pages: Pageable): JPanel() {

    private val counter = JLabel()

    /**
     * Draw components.
     */
    fun draw() {
        removeAll()
        listOf(
            Triple("<<<", -pages.getPageCount(), "First page"),
            Triple("<<", -10, "Back 10 pages"),
            Triple("<", -1, "Back 1 page"),
            Triple(">", 1, "Forward 1 page"),
            Triple(">>", 10, "Forward 10 pages"),
            Triple(">>>", pages.getPageCount(), "Last page"),
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
        counter.text = "${pages.getPageNumber()} / ${pages.getPageCount()}"
    }

    /**
     * Add a button.
     */
    private fun addButton(text: String, increment: Int, description: String) {
        add(JButton(text).also {
            it.toolTipText = description
            it.addActionListener {
                pages.setPageNumber(pages.getPageNumber() + increment)
                updateCounter()
            }
        })
    }
}
