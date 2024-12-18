package org.example

import java.util.PriorityQueue
import kotlin.io.path.Path
import kotlin.io.path.readLines
import kotlin.math.abs
import kotlin.time.measureTimedValue


const val MEMORY_SIZE = 71
const val NUM_BYTES = 1024

data class Vec2(val x: Int, val y: Int) {
    companion object {
        val up = Vec2(0, -1)
        val down = Vec2(0, 1)
        val right = Vec2(1, 0)
        val left = Vec2(-1, 0)
        val zero = Vec2(0, 0)
    }

    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
}

class Node(
    val position: Vec2,
    var cost: ULong,
    val heuristic: ULong,
    var predecessor: Node?,
)

enum class MemoryCell(val char: Char) {
    GOOD('.'),
    CORRUPTED('#'),
}

fun getNeighbors(position: Vec2, memory: List<List<MemoryCell>>): List<Vec2> {
    val height = memory.size
    val width = memory.first().size
    return arrayOf(Vec2.up, Vec2.down, Vec2.right, Vec2.left)
        .map { it + position }
        .filter { (x, y) -> x in 0..<width && y in 0..<height }
        .filter { (x, y) -> memory[y][x] == MemoryCell.GOOD }
}

fun heuristic(from: Vec2, to: Vec2) = (abs(from.x - to.x) + abs(from.y - to.y)).toULong()

fun aStar(memory: List<List<MemoryCell>>): List<Vec2> {
    val height = memory.size
    val width = memory.first().size

    val goal = Vec2(width - 1, height - 1)
    val start = Node(Vec2.zero, 0UL, heuristic(Vec2.zero, goal), null)

    val nodes = mutableMapOf(start.position to start)
    val toVisit = PriorityQueue<Node>(compareBy { it.cost + it.heuristic })
    toVisit.add(start)

    getNeighbors(start.position, memory)
        .forEach {
            toVisit.add(
                Node(
                    it,
                    1UL,
                    heuristic(it, goal),
                    start,
                )
            )
        }

    while (toVisit.isNotEmpty()) {
        val node = toVisit.poll()

        if (node.position == goal) {
            val path = mutableListOf(node.position)
            var current = node.predecessor
            while (current != null) {
                path.add(current.position)
                current = current.predecessor
            }
            path.reverse()
            return path
        }

        for (neighborPosition in getNeighbors(node.position, memory)) {
            val tentativeCost = node.cost + 1UL

            when (val oldNode = nodes[neighborPosition]) {
                null -> {
                    val newNode = Node(neighborPosition, tentativeCost, heuristic(neighborPosition, goal), node)
                    nodes[neighborPosition] = newNode
                    toVisit.add(newNode)
                }

                else -> {
                    val isCheaper = tentativeCost < oldNode.cost
                    if (isCheaper) {
                        oldNode.cost = tentativeCost
                        oldNode.predecessor = node

                        // Force heapify.
                        toVisit.remove(oldNode) // Only removes if existent.
                        toVisit.add(oldNode)
                    }
                }
            }
        }
    }

    return listOf()
}

fun visualizePath(memory: List<List<MemoryCell>>, path: List<Vec2>) {
    val positions = path.toSet()
    memory
        .forEachIndexed { rowIndex, row ->
            row.forEachIndexed { columnIndex, cell ->
                val position = Vec2(columnIndex, rowIndex)
                print(if (position in positions) 'O' else cell.char)
            }
            println()
        }
}

fun part1() {
    val memory = List(MEMORY_SIZE) { MutableList(MEMORY_SIZE) { MemoryCell.GOOD } }
    Path("input.txt")
        .readLines()
        .take(NUM_BYTES)
        .map { it.split(",").map(String::toInt) }
        .forEach { (x, y) -> memory[y][x] = MemoryCell.CORRUPTED }

    val path = aStar(memory)
    visualizePath(memory, path)
    println("num steps: ${path.size - 1}")
}

val memoryState = List(MEMORY_SIZE) { MutableList(MEMORY_SIZE) { MemoryCell.GOOD } }

fun getMemoryState(i: Int, positions: List<Vec2>): List<List<MemoryCell>> {
    @Suppress("NAME_SHADOWING")
    val positions = positions.take(i).toSet()

    memoryState
        .forEachIndexed { rowIndex, row ->
            row.forEachIndexed { columnIndex, cell ->
                memoryState[rowIndex][columnIndex] = if (Vec2(columnIndex, rowIndex) in positions) {
                    MemoryCell.CORRUPTED
                } else {
                    MemoryCell.GOOD
                }
            }
        }
    return memoryState
}

fun main() {
    part1()

    val positions = Path("input.txt")
        .readLines()
        .map { it.split(",").map(String::toInt) }
        .map { (x, y) -> Vec2(x, y) }

    positions
        .indices
        .windowed(2)
        .binarySearch { (i, j) ->
            val pathA = aStar(getMemoryState(i, positions))
            val pathB = aStar(getMemoryState(j, positions))
            if (pathA.isNotEmpty() && pathB.isEmpty()) {
                return@binarySearch 0
            }
            if (pathB.isNotEmpty()) {
                return@binarySearch -(positions.size - i - 1)
            }
            return@binarySearch i
        }
        .also { result ->
            println("${positions[result]}")
        }
}
