package org.example

import java.util.PriorityQueue
import kotlin.io.path.Path
import kotlin.io.path.readLines

const val COST_PER_ROTATION = 1000UL
const val COST_PER_STEP = 1UL

data class Vec2(val x: Int, val y: Int) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
}

enum class Direction(val vector: Vec2, val char: Char) {
    UP(Vec2(0, -1), '^'),
    DOWN(Vec2(0, 1), 'v'),
    LEFT(Vec2(-1, 0), '<'),
    RIGHT(Vec2(1, 0), '>');

    operator fun inc() = when (this) {
        UP -> RIGHT
        RIGHT -> DOWN
        DOWN -> LEFT
        LEFT -> UP
    }

    operator fun dec() = when (this) {
        UP -> LEFT
        LEFT -> DOWN
        DOWN -> RIGHT
        RIGHT -> UP
    }

    operator fun plus(offset: Int): Direction {
        var result = this
        for (i in 0..<offset) {
            ++result
        }
        return result
    }

    operator fun minus(offset: Int): Direction {
        var result = this
        for (i in 0..<offset) {
            --result
        }
        return result
    }
}

data class DijkstraData(val predecessor: Node?, val cost: ULong)

data class Node(val position: Vec2, val direction: Direction)

enum class TileType(val char: Char) {
    WALL('#'),
    START('S'),
    END('E'),
    EMPTY('.');

    companion object {
        fun fromChar(char: Char): TileType {
            for (tileType in TileType.entries) {
                if (char == tileType.char) {
                    return tileType
                }
            }
            throw IllegalArgumentException("Invalid tile type char: '$char'.")
        }
    }
}

data class Edge(val target: Node, val cost: ULong)

fun generateGraph(grid: List<List<TileType>>): Map<Node, Set<Edge>> {
    val graph = mutableMapOf<Node, Set<Edge>>()
    val width = grid.first().size
    val height = grid.size

    for (x in 0..<width) {
        for (y in 0..<height) {
            val tileType = grid[y][x]

            if (tileType == TileType.WALL) {
                continue
            }

            val position = Vec2(x, y)
            for (direction in Direction.entries) {
                graph[Node(position, direction)] = setOf(
                    Edge(
                        Node(position, direction + 1),
                        COST_PER_ROTATION,
                    ),
                    Edge(
                        Node(position, direction - 1),
                        COST_PER_ROTATION,
                    ),
                )
                    .union(
                        listOf(position + direction.vector)
                            .filter { grid[it.y][it.x] != TileType.WALL }
                            .map { Edge(Node(it, direction), COST_PER_STEP) }
                    )
            }
        }
    }

    return graph
}

fun findTileByType(tileType: TileType, grid: List<List<TileType>>) = grid
    .flatMapIndexed { rowIndex, row ->
        row.mapIndexed { columnIndex, tileType -> Vec2(columnIndex, rowIndex) to tileType }
    }
    .find { it.second == tileType }!!
    .first

data class Visitable(var node: Node, val cost: ULong, var origin: Node)

fun dijkstra(graph: Map<Node, Set<Edge>>, from: Node, to: Vec2): List<Node> {
    val visited = mutableSetOf(from)
    val toVisit = PriorityQueue<Visitable>(compareBy { it.cost })
    val dijkstraData = mutableMapOf<Node, DijkstraData>()

    graph[from]!!.forEach {
        toVisit.add(Visitable(it.target, it.cost, from))
        dijkstraData[it.target] = DijkstraData(from, it.cost)
    }

    while (toVisit.isNotEmpty()) {
        val (node, cost, predecessor) = toVisit.poll()

        if (node == from) {
            println("Discarding $node since it's the start.")
            continue
        }

        if (node in visited && cost >= dijkstraData[node]!!.cost) {
            println("Discarding $node because of too high costs.")
            continue
        }

        println("Exploring $node")
        if (node.position == to) {
            println("This is the target node.")
        }
        visited.add(node)
        dijkstraData[node] = DijkstraData(predecessor, cost)

        graph[node]!!
            .filter { it.target != dijkstraData[node]!!.predecessor }
            .forEach {
                toVisit.add(Visitable(it.target, cost + it.cost, node))
            }
    }

    val endNode = Direction
        .entries
        .map { Node(to, it) }
        .minByOrNull { dijkstraData[it]!!.cost }!!

    val route = mutableListOf(endNode)
    var current = endNode
    while (current != from) {
        current = dijkstraData[current]!!.predecessor!!
        route.add(current)
    }
    route.reverse()

    println("Cost: ${dijkstraData[route.last()]!!.cost}")

    return route
}

fun visualize(grid: List<List<TileType>>, route: List<Node>) {
    grid
        .forEachIndexed { rowIndex, row ->
            row.forEachIndexed { columnIndex, tileType ->
                val position = Vec2(columnIndex, rowIndex)
                val section = route
                    .find { it.position == position }
                if (section != null) {
                    print(section.direction.char)
                } else {
                    print(tileType.char)
                }
            }
            println()
        }
}

fun main() {
    val grid = Path("input.txt")
        .readLines()
        .map { line -> line.map(TileType::fromChar) }

    val (start, end) = findTileByType(TileType.START, grid) to findTileByType(TileType.END, grid)

    val graph = generateGraph(grid)

    val route = dijkstra(graph, Node(start, Direction.RIGHT), end)

//    visualize(grid, route)
}
