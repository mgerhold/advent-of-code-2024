package org.example

import java.util.PriorityQueue
import kotlin.io.path.Path
import kotlin.io.path.readLines

const val COST_PER_ROTATION = 1000UL
const val COST_PER_STEP = 1UL

data class Vec2(val x: Int, val y: Int) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)

    companion object {
        val up = Vec2(0, -1)
        val right = Vec2(1, 0)
        val down = Vec2(0, 1)
        val left = Vec2(-1, 0)
    }

    fun toDirection() = when (this) {
        up -> Direction.UP
        right -> Direction.RIGHT
        down -> Direction.DOWN
        left -> Direction.LEFT
        else -> throw IllegalArgumentException("Vector $this cannot be converted into a direction.")
    }
}

enum class Direction {
    UP,
    RIGHT,
    DOWN,
    LEFT;

    operator fun unaryMinus() = when (this) {
        UP -> DOWN
        RIGHT -> LEFT
        DOWN -> UP
        LEFT -> RIGHT
    }
}

data class Reindeer(val position: Vec2, val direction: Direction)

data class Predecessor(val position: Vec2, val direction: Direction, val cost: ULong)

sealed class Tile {
    companion object {
        fun fromChar(char: Char) =
            when (char) {
                Wall.getChar() -> Wall
                End.getChar(), Empty.getChar(), Start.getChar() -> Empty
                else -> throw IllegalArgumentException("Invalid tile char '$char'.")
            }
    }

    object Wall : Tile() {
        fun getChar() = '#'
    }

    object End : Tile() {
        fun getChar() = 'E'
    }

    object Start : Tile() {
        fun getChar() = 'S'
    }

    object Empty : Tile() {
        fun getChar() = '.'
    }

    class Explored(val predecessors: MutableSet<Predecessor>) : Tile()

    override fun toString() =
        when (this) {
            is Wall -> getChar()
            is Start -> getChar()
            is Empty -> getChar()
            is Explored -> 'E'
            is End -> throw RuntimeException("Map must not contain the End tile.")
        }
            .toString()
}

class Maze private constructor(
    private val tiles: List<MutableList<Tile>>,
    val startPosition: Vec2,
    val endPosition: Vec2
) {
    companion object {
        fun fromStrings(strings: List<String>) = Maze(
            strings
                .map { it.map(Tile::fromChar).toMutableList() },
            strings
                .flatMapIndexed { rowIndex, row ->
                    row.mapIndexed { columnIndex, char ->
                        Vec2(
                            columnIndex,
                            rowIndex
                        ) to char
                    }
                }
                .first { (_, char) -> char == Tile.Start.getChar() }
                .first,
            strings
                .flatMapIndexed { rowIndex, row ->
                    row.mapIndexed { columnIndex, char ->
                        Vec2(
                            columnIndex,
                            rowIndex
                        ) to char
                    }
                }
                .first { (_, char) -> char == Tile.End.getChar() }
                .first
        )
    }

    private val width = tiles.first().size

    private val height = tiles.size

    operator fun get(position: Vec2) = tiles[position.y][position.x]

    operator fun set(position: Vec2, tile: Tile) {
        tiles[position.y][position.x] = tile
    }

    override fun toString() = tiles
        .mapIndexed { rowIndex, row ->
            row.mapIndexed { columnIndex, tile ->
                val position = Vec2(columnIndex, rowIndex)
                if (position == startPosition) {
                    return@mapIndexed 'S'
                }
                if (position == endPosition) {
                    return@mapIndexed 'E'
                }
                when (tile) {
                    is Tile.Empty -> Tile.Empty.getChar()
                    is Tile.Wall -> Tile.Wall.getChar()
                    is Tile.Explored -> 'x'
                    else -> throw RuntimeException("Unexpected Tile instance.")
                }
            }
                .joinToString("")
        }
        .joinToString("\n")

    fun toStringWithPath(): String {
        val path = mutableListOf(endPosition)

        var current = endPosition
        while (current != startPosition) {
            val tile = this[current] as Tile.Explored
            val predecessor = tile
                .predecessors
                .map { (position, direction, _) ->
                    position to calculateCost(Reindeer(position, direction), current)
                }
                .minByOrNull { (_, cost) -> cost }!!
                .first

            path.add(predecessor)
            current = predecessor
        }
        path.add(startPosition)

        println(path)

        var result = ""
        for (row in 0..<height) {
            var line = ""
            for (column in 0..<width) {
                val position = Vec2(column, row)

                if (position == startPosition) {
                    line += 'S'
                    continue
                }

                if (position == endPosition) {
                    line += 'E'
                    continue
                }

                val pathIndex = path.indexOf(position)
                if (pathIndex != -1) {
                    val predecessor = path[pathIndex + 1]
                    val direction = (position - predecessor).toDirection()
                    line += when (direction) {
                        Direction.UP -> '^'
                        Direction.DOWN -> 'v'
                        Direction.RIGHT -> '>'
                        Direction.LEFT -> '<'
                    }
                    continue
                }

                line += when (this[position]) {
                    is Tile.Wall -> '#'
                    is Tile.Empty -> ' '
                    is Tile.Explored -> ' '
                    else -> throw RuntimeException("Unexpected Tile instance: ${this[position].javaClass}")
                }
            }
            result += line + "\n"
        }
        return result
    }
}

fun getNeighborPositions(maze: Maze, position: Vec2): List<Vec2> {
    return arrayOf(
        position + Vec2.up,
        position + Vec2.down,
        position + Vec2.left,
        position + Vec2.right,
    )
        .filter { maze[it] is Tile.Empty || maze[it] is Tile.Explored }
}

fun requiredRotations(reindeer: Reindeer, direction: Direction): ULong {
    if (reindeer.direction == direction) {
        return 0UL
    }
    if (reindeer.direction == -direction) {
        return 2UL
    }
    return 1UL
}

fun calculateCost(reindeer: Reindeer, target: Vec2): ULong {
    require(
        target - reindeer.position in arrayOf(
            Vec2.up,
            Vec2.down,
            Vec2.left,
            Vec2.right,
        )
    )

    val delta = target - reindeer.position
    val direction = delta.toDirection()
    val numRotations = requiredRotations(reindeer, direction)
    return numRotations * COST_PER_ROTATION + COST_PER_STEP
}

data class Visitable(val position: Vec2, val cost: ULong, val origin: Vec2, val direction: Direction)

fun findPath(maze: Maze) {
    maze[maze.startPosition] = Tile.Explored(mutableSetOf())
    val visited = mutableSetOf(maze.startPosition)

    val toVisit = PriorityQueue<Visitable>(compareBy { it.cost })
    toVisit.addAll(
        getNeighborPositions(maze, maze.startPosition)
            .map {
                Visitable(
                    it,
                    calculateCost(
                        Reindeer(maze.startPosition, Direction.RIGHT),
                        it,
                    ),
                    maze.startPosition,
                    Direction.RIGHT,
                )
            }
    )

    while (toVisit.isNotEmpty()) {
        val (neighborPosition, cost, origin, originDirection) = toVisit.poll()

        if (neighborPosition == maze.startPosition) {
            continue
        }

        if (
            neighborPosition in visited
            && (maze[origin] as Tile.Explored)
                .predecessors
                .any { it.position == neighborPosition }
        ) {
//            println("Discarding position $neighborPosition")
            continue
        }

        val predecessor = Predecessor(
            origin,
            originDirection,
            cost
        )
        when (val tile = maze[neighborPosition]) {
            is Tile.Explored -> tile.predecessors.add(predecessor)
            else -> maze[neighborPosition] = Tile.Explored(mutableSetOf(predecessor))
        }

        visited.add(neighborPosition)
//        println("Explored tile at $neighborPosition (coming from ${origin}), total cost: $cost")
        if (neighborPosition == maze.endPosition) {
            println("This is the destination.")
        }

        val predecessors = (maze[neighborPosition] as Tile.Explored).predecessors

        toVisit.addAll(
            getNeighborPositions(maze, neighborPosition)
                .filter { it != origin }
                .map {
                    it to predecessors
                        .minOfOrNull { (predecessorPosition, predecessorDirection, predecessorCost) ->
//                            val incomingCost =
//                                calculateCost(Reindeer(predecessorPosition, predecessorDirection), neighborPosition)
                            val currentDirection = (neighborPosition - predecessorPosition).toDirection()
                            val outgoingCost = calculateCost(Reindeer(neighborPosition, currentDirection), it)
                            predecessorCost /*+ incomingCost*/ + outgoingCost
                        }!!
                }
                .map { (position, cost) ->
                    Visitable(
                        position,
                        cost,
                        neighborPosition,
                        (neighborPosition - origin).toDirection(),
                    )
                }
        )
    }

    println(maze.toStringWithPath())
    val result = (maze[maze.endPosition] as Tile.Explored)
        .predecessors
        .minOfOrNull { it.cost + calculateCost(Reindeer(it.position, it.direction), maze.endPosition) }!!
    println("Cost: ${result - 1UL}")
}

fun main() {
    val maze = Path("input.txt")
        .readLines()
        .let(Maze::fromStrings)
        .also(::println)
    findPath(maze)
}
