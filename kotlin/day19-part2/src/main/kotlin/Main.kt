package org.example

import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.time.measureTimedValue

typealias Node = ULong

//data class Transition(val color: Color, val sourcePattern: String?)

data class Edge(val from: Node, val colors: Set<Color>, var to: Node)

data class Graph(val edges: Set<Edge>)

typealias Design = String

fun validateDesign(
    graph: Graph,
    childrenMapping: Map<Node, List<Edge>>,
    startNodes: Map<Node, ULong>,
    design: Design,
): ULong {
    if (design.length == 1) {
        return startNodes
            .entries
            .filter { (key, _) -> key == 0UL }
            .sumOf { (_, value) -> value }
    }

    val nextStartNodes = startNodes
        .flatMap { (startNode, count) ->
            childrenMapping[startNode]!!
                .filter { edge -> design[1] in edge.colors }
                .map { edge -> edge.to to count }
        }
        .groupingBy { (node, count) -> node }
        .fold(0UL) { sum, element -> sum + element.second }

    if (nextStartNodes.isEmpty()) {
        return 0UL
    }

    return validateDesign(
        graph,
        childrenMapping,
        nextStartNodes,
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
        edges.add(
            Edge(
                convergenceNode,
                mutableSetOf(lastColor),
                lastNode
            )
        )
    }

    for (node in nodes) {
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
    }

    val targets = edges.map { it.to }.toSet()
    // Remove all edges that come from a node that's not targeted by any other edge.
    edges.removeIf { it.from !in targets }
    // Also remove those nodes from the start nodes.
    startNodes.removeAll { it.id !in targets }

    val collapsedEdges = edges
        .groupingBy { it.from to it.to }
        .fold(setOf<Color>()) { colors, edge -> colors.union(edge.colors) }
        .entries
        .map { (key, value) -> Edge(key.first, value, key.second) }
        .toSet()

    return Graph(collapsedEdges) to startNodes
}

fun printForGraphviz(graph: Graph, startNodes: Set<StartNode>) {
    println("digraph G {")
    for ((from, colors, to) in graph.edges) {
        val label = colors.joinToString("")
        println("  $from -> $to [ label=\"$label\" ];")
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

    val nodes = graph
        .edges
        .map { it.from }

    val childrenMapping = nodes
        .associateWith { node ->
            graph
                .edges
                .filter { it.from == node }
        }

    measureTimedValue {
        designs
            .mapIndexed { i, design ->
                val matchingNodes = startNodes
                    .filter { node -> design.first() in node.entryColors }
                    .associate { node -> node.id to 1UL }
                validateDesign(graph, childrenMapping, matchingNodes, design)
            }
            .sum()
    }.also(::println)
}
