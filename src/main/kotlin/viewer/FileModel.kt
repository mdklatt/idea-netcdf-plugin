package dev.mdklatt.idea.netcdf.viewer

import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import ucar.nc2.*
import kotlin.sequences.Sequence
import dev.mdklatt.idea.netcdf.*


/**
 * Map a netCDF file schema to a tree structure.
 */
internal class FileModel() : DefaultTreeModel(null) {
    /**
     * Base class for file nodes.
     */
    abstract class Node<out T>(protected val fileNode: T) where T: CDMNode, T: AttributeContainer {
        /** Node display label. */
        open val name: String = fileNode.shortName

        /** Node attributes. */
        val attributes: Sequence<String>
            get() = fileNode.attributes.sortedBy { it.fullName }.map {
                "${it.fullName}: ${it.stringValue}"
            }.asSequence()

        /**
         * String representation.
         *
         * @return: string value
         */
        override fun toString() = name
    }

    /**
     * File node for a netCDF group.
     */
    class Group(group: ucar.nc2.Group, val isRoot: Boolean = false) : Node<ucar.nc2.Group>(group) {
        /** All groups defined under this group. */
        val groups = fileNode.groups.map { Group(it) }.sortedBy { it.name }.asSequence()

        /** All variables defined under this group. */
        val variables = fileNode.variables.map { Variable(it) }.sortedBy { it.name }.asSequence()
    }

    /**
     * File node for a netCDF variable.
     */
    class Variable(variable: ucar.nc2.Variable) : Node<ucar.nc2.Variable>(variable) {
        /** Full path to variable within netCDF file. */
        override val name: String
            get() = fileNode.fullName

        /** Variable dimensions. */
        val dimensions: Sequence<String>
            get() {
                return fileNode.publicDimensions.map {
                    val unlimited = if (it.isUnlimited) "(unlimited)" else ""
                    "${it.fullName}[${it.length}] $unlimited"
                }.asSequence()
            }

        private val shape: Sequence<Int>
            get() = fileNode.publicDimensions.map { it.length }.asSequence()

        /**
         * String representation.
         *
         * @return: string value
         */
        override fun toString(): String {
            val dims = shape.map { it.toString() }.joinToString(", ")
            return "${fileNode.shortName}: ${fileNode.typeString}[${dims}]"
        }
    }

    private var fileRoot: Group? = null

    /**
     * Populate tree nodes from a netCDF file.
     *
     * file: open netCDF file
     */
    fun fillTree(file: NetcdfFile) {
        clearTree()
        root = DefaultMutableTreeNode(file.location)
        fileRoot = Group(file.rootGroup, true).also {
            addGroup((root as DefaultMutableTreeNode), it)
        }
    }

    /**
     * Remove all tree nodes.
     */
    fun clearTree() {
        (root as DefaultMutableTreeNode?)?.removeAllChildren()
    }

    /**
     * Add a group node to the tree.
     *
     * @param head: parent node
     * @param group group node
     */
    private fun addGroup(head: DefaultMutableTreeNode, group: Group) {
        val node: DefaultMutableTreeNode
        if (group.isRoot) {
            node = head
        } else {
            node = DefaultMutableTreeNode(group)
            head.add(node)
        }
        group.attributes.forEach { node.add(DefaultMutableTreeNode(it)) }
        if (group.groups.count() > 0) {
            DefaultMutableTreeNode("Groups").let {
                node.add(it)
                group.groups.forEach { sub -> addGroup(it, sub) }
            }
        }
        addVariables(node, group.variables)
    }

    /**
     * Add variable nodes to the tree.
     *
     * @param head: parent group node
     * @param variables: variable nodes
     */
    private fun addVariables(head: DefaultMutableTreeNode, variables: Sequence<Variable>) {
        if (variables.count() == 0) {
            return
        }
        DefaultMutableTreeNode("Variables").let { node ->
            head.add(node)
            variables.forEach {
                DefaultMutableTreeNode(it).apply {
                    node.add(this)
                    it.attributes.forEach { this.add(DefaultMutableTreeNode(it)) }
                    addDimensions(this, it.dimensions)
                }
            }
        }
    }

    /**
     * Add dimension nodes to a tree.
     *
     * @param head: parent variable node
     * @param dimensions: dimension descriptors
     */
    private fun addDimensions(head: DefaultMutableTreeNode, dimensions : Sequence<String>) {
        if (dimensions.count() == 0) {
            return
        }
        DefaultMutableTreeNode("Dimensions").let { node ->
            head.add(node)
            dimensions.forEach { node.add(DefaultMutableTreeNode(it)) }
        }
    }
}
