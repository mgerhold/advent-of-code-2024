package org.example

import kotlin.io.path.Path
import kotlin.io.path.readLines

class NodePart1(val height: Int, var children: MutableSet<NodePart1>)

class NodePart2(val height: Int, var children: MutableList<NodePart2>)

data class Point(val x: Int, val y: Int)

enum class Transition {
    FORWARD,
    BACKWARD,
    NONE,
}

fun printTopology(topology: List<List<Int>>) {
    for ((rowIndex, row) in topology.withIndex()) {
        for ((columnIndex, height) in row.withIndex()) {
            val horizontal = when {
                columnIndex < row.indices.last() && height != -1 && row[columnIndex + 1] == height + 1 ->
                    Transition.FORWARD

                columnIndex < row.indices.last() && height != 0 && row[columnIndex + 1] == height - 1 ->
                    Transition.BACKWARD

                else -> Transition.NONE
            }
            print(if (height == -1) '.' else height)
            print(
                when (horizontal) {
                    Transition.FORWARD -> '>'
                    Transition.BACKWARD -> '<'
                    Transition.NONE -> ' '
                }
            )
        }
        println()
        for ((columnIndex, height) in row.withIndex()) {
            val vertical = when {
                rowIndex < topology.indices.last() && height != -1 && topology[rowIndex + 1][columnIndex] == height + 1 ->
                    Transition.FORWARD

                rowIndex < topology.indices.last() && height != 0 && topology[rowIndex + 1][columnIndex] == height - 1 ->
                    Transition.BACKWARD

                else -> Transition.NONE
            }
            print(
                when (vertical) {
                    Transition.FORWARD -> 'v'
                    Transition.BACKWARD -> '^'
                    Transition.NONE -> ' '
                }
            )
            print(' ')
        }
        println()
    }
}

fun printTopology(nodes: Map<Point, NodePart1>, topologyWidth: Int, topologyHeight: Int) {
    val topology = mutableListOf<List<Int>>()
    for (rowIndex in 0..<topologyHeight) {
        val row = mutableListOf<Int>()
        for (columnIndex in 0..<topologyWidth) {
            val height = nodes[Point(columnIndex, rowIndex)]?.height ?: -1
            row.add(height)
        }
        topology.add(row)
    }
    printTopology(topology)
}

fun createDotOutputPart1(nodes: Map<Point, NodePart1>): String {
    val nodeName = { point: Point -> "node_${point.x}_${point.y}" }
    var result = "digraph G {\n"

    for ((point, node) in nodes) {
        result += "  ${nodeName(point)} [label=\"${node.height}\"];\n"
    }

    result += "\n"

    for ((point, node) in nodes) {
        for (child in node.children) {
            val childPosition = nodes
                .entries
                .first { (_, node) -> node == child }
                .key
            result += "  ${nodeName(point)} -> ${nodeName(childPosition)};\n"
        }
    }

    result += "}\n"
    return result
}

fun createDotOutputPart2(nodes: Map<Point, NodePart2>): String {
    val nodeName = { point: Point -> "node_${point.x}_${point.y}" }
    var result = "digraph G {\n"

    for ((point, node) in nodes) {
        result += "  ${nodeName(point)} [label=\"${node.height}\"];\n"
    }

    result += "\n"

    for ((point, node) in nodes) {
        for (child in node.children) {
            val childPosition = nodes
                .entries
                .first { (_, node) -> node == child }
                .key
            result += "  ${nodeName(point)} -> ${nodeName(childPosition)};\n"
        }
    }

    result += "}\n"
    return result
}

fun readInput() = Path("input.txt")
    .readLines()
    .map { it.toList().map(Char::digitToInt) }

fun buildGraphPart1(input: List<List<Int>>): Map<Point, NodePart1> {
    val nodes = input
        .indices
        .flatMap { rowIndex -> input[rowIndex].indices.map { columnIndex -> Point(columnIndex, rowIndex) } }
        .associateWith { point -> NodePart1(input[point.y][point.x], mutableSetOf()) }

    for ((rowIndex, row) in input.withIndex()) {
        for ((columnIndex, height) in row.withIndex()) {
            val node = nodes[Point(columnIndex, rowIndex)]!!

            if (columnIndex < row.indices.last()) {
                when (row[columnIndex + 1]) {
                    height + 1 -> node.children.add(nodes[Point(columnIndex + 1, rowIndex)]!!)
                    height - 1 -> nodes[Point(columnIndex + 1, rowIndex)]!!.children.add(node)
                }
            }

            if (rowIndex < input.indices.last()) {
                when (input[rowIndex + 1][columnIndex]) {
                    height + 1 -> node.children.add(nodes[Point(columnIndex, rowIndex + 1)]!!)
                    height - 1 -> nodes[Point(columnIndex, rowIndex + 1)]!!.children.add(node)
                }
            }
        }
    }

    return nodes
}

fun buildGraphPart2(input: List<List<Int>>): Map<Point, NodePart2> {
    val nodes = input
        .indices
        .flatMap { rowIndex -> input[rowIndex].indices.map { columnIndex -> Point(columnIndex, rowIndex) } }
        .associateWith { point -> NodePart2(input[point.y][point.x], mutableListOf()) }

    for ((rowIndex, row) in input.withIndex()) {
        for ((columnIndex, height) in row.withIndex()) {
            val node = nodes[Point(columnIndex, rowIndex)]!!

            if (columnIndex < row.indices.last()) {
                when (row[columnIndex + 1]) {
                    height + 1 -> node.children.add(nodes[Point(columnIndex + 1, rowIndex)]!!)
                    height - 1 -> nodes[Point(columnIndex + 1, rowIndex)]!!.children.add(node)
                }
            }

            if (rowIndex < input.indices.last()) {
                when (input[rowIndex + 1][columnIndex]) {
                    height + 1 -> node.children.add(nodes[Point(columnIndex, rowIndex + 1)]!!)
                    height - 1 -> nodes[Point(columnIndex, rowIndex + 1)]!!.children.add(node)
                }
            }
        }
    }

    return nodes
}

fun part1(nodes: Map<Point, NodePart1>): Int {
    @Suppress("NAME_SHADOWING")
    var nodes = nodes

    while (true) {
        val oldNodesCount = nodes.size

        val nodesWithoutParentsPositions = nodes
            .asSequence()
            .filter { (_, node) -> node.height != 0 }
            .filter { (_, currentNode) -> nodes.none { (_, node) -> currentNode in node.children } }
            .map { (position, _) -> position }
            .toSet()

        nodes = nodes.filterKeys { it !in nodesWithoutParentsPositions }

        if (nodes.size == oldNodesCount) {
            break
        }
    }

    while (true) {
        val oldNodesCount = nodes.size

        val nodesWithoutChildrenPositions = nodes
            .filter { (_, node) -> node.children.size == 0 && node.height != 9 }
            .map { (position, _) -> position }
            .toSet()
        val nodesToRemove = nodesWithoutChildrenPositions
            .map { nodes[Point(it.x, it.y)]!! }

        nodes
            .forEach { (_, node) -> node.children.removeIf { child -> child in nodesToRemove } }

        nodes = nodes.filterKeys { it !in nodesWithoutChildrenPositions }

        if (nodes.size == oldNodesCount) {
            break
        }
    }

    for (height in 1..<9) {
        val nodesToMerge = nodes
            .filter { (_, node) -> node.height == height }
            .map { (_, node) -> node to nodes.filter { (_, subNode) -> node in subNode.children }.map { (_, subNode) -> subNode } }

        for ((child, parents) in nodesToMerge) {
            for (parent in parents) {
                parent.children.remove(child)
                parent.children.addAll(child.children)
            }
            nodes = nodes.filterValues { it != child }
        }
    }

//    println(createDotOutputPart1(nodes))

    return nodes
        .values
        .filter { it.height == 0 }
        .sumOf { it.children.size }
}

fun part2(nodes: Map<Point, NodePart2>): Int {
    @Suppress("NAME_SHADOWING")
    var nodes = nodes

    while (true) {
        val oldNodesCount = nodes.size

        val nodesWithoutParentsPositions = nodes
            .asSequence()
            .filter { (_, node) -> node.height != 0 }
            .filter { (_, currentNode) -> nodes.none { (_, node) -> currentNode in node.children } }
            .map { (position, _) -> position }
            .toSet()

        nodes = nodes.filterKeys { it !in nodesWithoutParentsPositions }

        if (nodes.size == oldNodesCount) {
            break
        }
    }

    while (true) {
        val oldNodesCount = nodes.size

        val nodesWithoutChildrenPositions = nodes
            .filter { (_, node) -> node.children.size == 0 && node.height != 9 }
            .map { (position, _) -> position }
            .toSet()
        val nodesToRemove = nodesWithoutChildrenPositions
            .map { nodes[Point(it.x, it.y)]!! }

        nodes
            .forEach { (_, node) -> node.children.removeIf { child -> child in nodesToRemove } }

        nodes = nodes.filterKeys { it !in nodesWithoutChildrenPositions }

        if (nodes.size == oldNodesCount) {
            break
        }
    }

    for (height in 1..<9) {
        val nodesToMerge = nodes
            .filter { (_, node) -> node.height == height }
            .map { (_, node) -> node to nodes.filter { (_, subNode) -> node in subNode.children }.map { (_, subNode) -> subNode } }

        for ((child, parents) in nodesToMerge) {
            for (parent in parents) {
                val numOccurrences = parent.children.count { it == child }
                parent.children.removeIf { it == child }
                for (i in 0..<numOccurrences) {
                    parent.children.addAll(child.children)
                }
            }
            nodes = nodes.filterValues { it != child }
        }
    }

//    println(createDotOutputPart2(nodes))

    return nodes
        .values
        .filter { it.height == 0 }
        .sumOf { it.children.size }
}

fun main() {
    val input = readInput()

    val graphPart1 = input.let(::buildGraphPart1)
    val resultPart1 = part1(graphPart1)
    println(resultPart1)

    val graphPart2 = input.let(::buildGraphPart2)
    val resultPart2 = part2(graphPart2)
    println(resultPart2)
}
