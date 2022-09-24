package dev.mdklatt.idea.netcdf

import ucar.nc2.AttributeContainer
import ucar.nc2.CDMNode
import ucar.nc2.NetcdfFile
import kotlin.sequences.Sequence


/**
 * Tree view of a netCDF file schema.
 */
internal class FileView(file: NetcdfFile) {
    /**
     * Base class for tree nodes.
     */
    abstract class Node<out T>(protected val fileNode: T) where T: CDMNode, T: AttributeContainer {
        /** Node display label. */
        open val name: String = fileNode.fullNameEscaped

        /** Node attributes. */
        val attributes: Sequence<String>
            get() = fileNode.attributes.sortedBy { it.fullNameEscaped }.map {
                "${it.fullNameEscaped}: ${it.stringValue}"
            }.asSequence()

        /**
         * String representation.
         *
         * @return: string value
         */
        override fun toString() = name
    }

    /**
     * Node for a netCDF group.
     */
    class Group(group: ucar.nc2.Group, val isRoot: Boolean = false) : Node<ucar.nc2.Group>(group) {
        /** All groups defined under this group. */
        val groups = fileNode.groups.map { Group(it) }.sortedBy { it.name }.asSequence()

        /** All variables defined under this group. */
        val variables = fileNode.variables.map { Variable(it) }.sortedBy { it.name }.asSequence()
    }

    /**
     * Node for a netCDF variable.
     */
    class Variable(variable: ucar.nc2.Variable) : Node<ucar.nc2.Variable>(variable) {
        /** Variable dimensions. */
        val dimensions: Sequence<String>
            get() {
                return fileNode.publicDimensions.map {
                    val unlimited = if (it.isUnlimited) "(unlimited)" else ""
                    "${it.fullNameEscaped}[${it.length}] $unlimited"
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
            return "${fileNode.nameEscaped}: ${fileNode.typeString}[${dims}]"
        }
    }

    /** File root group. */
    val root = Group(file.rootGroup, true)
}
