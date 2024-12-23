package org.example

import kotlin.io.path.Path
import kotlin.io.path.readLines

data class Connection(val from: String, val to: String)

fun printForGraphviz(connections: List<Connection>) {
    println("graph G {")
    for ((from, to) in connections) {
        println("  $from -- $to")
    }
    println("}")
}

val cache = mutableMapOf<Set<String>, List<String>>()

fun largestConnectedSubset(
    nodes: Set<String>,
    topology: Map<String, Set<String>>
): List<String> {
    return cache
        .getOrPut(nodes) {
            nodes
                .map { node ->
                    node to topology[node]!!.intersect(nodes)
                }
                .map { (node, neighbors) ->
                    if (neighbors.isEmpty()) {
                        listOf(node)
                    } else {
                        (largestConnectedSubset(neighbors, topology) + node).sorted()
                    }
                }
                .maxBy { it.size }
        }
}

fun part1(topology: Map<String, Set<String>>) {
    val triplets = mutableSetOf<List<String>>()

    for (node in topology.keys) {
        var paths = topology[node]!!
            .map { listOf(node, it) }

        for (i in 0..<2) {
            paths = paths
                .flatMap { path ->
                    topology[path.last()]!!
                        .filter { it != path[path.size - 2] }
                        .map { path + it }
                }
        }

        paths
            .filter { it.first() == it.last() }
            .map { it.subList(0, it.size - 1) }
            .map { it.sorted() }
            .forEach { triplets.add(it) }
    }

    triplets
        .filter {
            it.any { computer ->
                computer.startsWith("t")
            }
        }
        .count()
        .also(::println)
}

fun parseConnections() = Path("input.txt")
    .readLines()
    .map { line ->
        line
            .split("-")
            .let { (first, second) -> Connection(first, second) }
    }

fun buildTopology(connections: List<Connection>) = connections
    .flatMap { connection ->
        listOf(connection, Connection(connection.to, connection.from))
    }
    .groupingBy { it.from }
    .fold(setOf<String>()) { set, connection ->
        set + connection.to
    }

fun part2(topology: Map<String, Set<String>>) {
    largestConnectedSubset(topology.keys, topology)
        .joinToString(",")
        .also(::println)
}

fun main() {
    val connections = parseConnections()
//    printForGraphviz(connections)
    val topology = buildTopology(connections)

    part1(topology)
    part2(topology)
}
