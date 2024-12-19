package org.example

import kotlin.io.path.Path
import kotlin.io.path.readText

typealias Node = ULong

data class Edge(val from: Node, val colors: Set<Char>, var to: Node)

data class Graph(val edges: Set<Edge>)

typealias Design = String

fun validateDesign(
    graph: Graph,
    startNodes: Set<StartNode>,
    design: Design,
): Boolean {
    if (design.isEmpty()) {
        return true
    }

    val possibleStartNodes = startNodes
        .filter { node -> design.first() in node.entryColors }

    if (possibleStartNodes.isEmpty()) {
        return false
    }

    val newStartNodes = possibleStartNodes
        .flatMap { startNode ->
            graph
                .edges
                .filter { edge -> edge.from == startNode.id }
                .map { edge -> StartNode(edge.to, edge.colors.toMutableSet()) }
        }
        .toSet()

    return validateDesign(
        graph,
        newStartNodes,
        design.substring(1)
    )
}

typealias Color = Char

data class StartNode(val id: Node, val entryColors: MutableSet<Color>)

fun buildGraph(patterns: List<String>): Pair<Graph, Set<StartNode>> {
    val startNodes = mutableSetOf<StartNode>()
    val edges = mutableSetOf<Edge>()

    val indexSequence = generateSequence(0UL) { it + 1UL }
    val indexIterator = indexSequence.iterator()

    val convergenceNode = indexIterator.next()
    val nodes = mutableSetOf(convergenceNode)

    for (pattern in patterns) {
        var lastNode = convergenceNode
        var lastColor = pattern.last()
        for (colors in pattern.dropLast(1).reversed()) {
            val node = indexIterator.next()
            nodes.add(node)
            edges.add(
                Edge(
                    node,
                    mutableSetOf(lastColor),
                    lastNode,
                )
            )
            lastNode = node
            lastColor = colors
        }
        when (val existingStartNode = startNodes.find { it.id == lastNode }) {
            null -> startNodes.add(StartNode(lastNode, mutableSetOf(lastColor)))
            else -> existingStartNode.entryColors.add(lastColor)
        }
        edges.add(Edge(convergenceNode, mutableSetOf(lastColor), lastNode))
    }

    /*for (node in nodes) {
        val children = edges
            .filter { it.from == node }
            .map { it.colors to it.to }
            .toSet()
//        println("children of $node: $children")
        for (other in nodes) {
            if (node == other) {
                break
            }
            val otherChildren = edges
                .filter { it.from == other }
                .map { it.colors to it.to }
                .toSet()
//            println("children of other node $other: $otherChildren")

            if (children == otherChildren) {
//                println("node $node ($children) has the same children as node $other ($otherChildren)")
                edges
                    .filter { it.to == other }
                    .forEach { it.to = node }

                val nodeToRemove = startNodes.find { startNode ->
                    startNode.id == other
                }
                if (nodeToRemove != null) {
//                    println("$nodeToRemove is a starting node that should be removed")
                    startNodes.remove(nodeToRemove)

                    when (val remainingStartNode = startNodes.find { startNode ->
                        startNode.id == node
                    }) {
                        null -> startNodes.add(StartNode(node, nodeToRemove.entryColors))
                        else -> remainingStartNode.entryColors.addAll(nodeToRemove.entryColors)
                    }
                }
            }
        }
    }*/

    val targets = edges.map { it.to }.toSet()
    // Remove all edges that come from a node that's not targeted by any other edge.
    edges.removeIf { it.from !in targets }
    // Also remove those nodes from the start nodes.
    startNodes.removeAll { it.id !in targets }

    val collapsedEdges = edges
        .groupingBy { it.from to it.to }
        .fold(setOf<Char>()) { colors, edge -> colors.union(edge.colors) }
        .entries
        .map { (key, value) -> Edge(key.first, value, key.second) }
        .toSet()

    return Graph(collapsedEdges) to startNodes
}

fun printForGraphviz(graph: Graph, startNodes: Set<StartNode>) {
    println("digraph G {")
    for ((from, colors, to) in graph.edges) {
        println("  $from -> $to [ label=\"${colors.joinToString("")}\" ];")
    }
    println()

    for (startNode in startNodes) {
        println("  ${startNode.id} [shape=Mdiamond, label=\"${startNode.id}, ${startNode.entryColors.joinToString("")}\"];")
    }

    println("}")
}

fun main() {
    val input = Path("input.txt").readText().trim()

    val patterns = input
        .substringBefore("\n\n")
        .split(", ")

    val designs = input
        .substringAfter("\n\n")
        .lines()
        .filter { !it.startsWith("//") }

    val (graph, startNodes) = buildGraph(patterns)

    printForGraphviz(graph, startNodes)


    val numPossibleDesigns = designs
        .map { design -> design to validateDesign(graph, startNodes, design) }
        .onEach { (design, result) ->
//            println("$design: ${if (result) "skibidi no cap" else "toilet"}")
        }
        .count { (_, result) -> result }

    println(numPossibleDesigns)
}
