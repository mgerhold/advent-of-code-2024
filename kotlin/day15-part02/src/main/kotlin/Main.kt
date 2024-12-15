package org.example

import java.lang.Thread.sleep
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

enum class TileType(private val char: Char) {
    WALL('#'),
    BOX_LEFT('['),
    BOX_RIGHT(']'),
    ROBOT('@'),
    EMPTY('.');

    companion object {
        fun fromChar(char: Char) = TileType
            .entries
            .find { it.char == char } ?: throw IllegalArgumentException("Invalid map char: '$char'")
    }

    override fun toString() = if (this == TileType.EMPTY) " " else char.toString()
}

enum class Direction(private val char: Char, val vector: Vec2) {
    UP('^', Vec2(0, -1)),
    RIGHT('>', Vec2(1, 0)),
    DOWN('v', Vec2(0, 1)),
    LEFT('<', Vec2(-1, 0));

    companion object {
        fun fromChar(char: Char) = Direction
            .entries
            .find { it.char == char } ?: throw IllegalArgumentException("Invalid direction char: '$char'")
    }

    override fun toString() = char.toString()
}

data class Vec2(val x: Int, val y: Int) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
}

class Warehouse private constructor(private val tiles: List<MutableList<TileType>>) {
    val sumOfGpsCoordinates: Int
        get() = tiles
            .flatMapIndexed { rowIndex, row ->
                row
                    .mapIndexed { columnIndex, tileType ->
                        Vec2(columnIndex, rowIndex) to tileType
                    }
                    .filter { (_, tileType) -> tileType == TileType.BOX_LEFT }
            }
            .map { it.first }
            .sumOf { it.x + 100 * it.y }

    val grid: List<List<TileType>> = tiles
        .map { it.toList() }
        .toList()

    private val robotPosition: Vec2
        get() = tiles
            .flatMapIndexed { lineIndex, line ->
                line
                    .mapIndexed { columnIndex, tileType ->
                        Vec2(columnIndex, lineIndex) to tileType
                    }
            }
            .find { (_, tileType) -> tileType == TileType.ROBOT }!!
            .first

    companion object {
        fun fromLines(lines: List<String>) = Warehouse(
            lines.map { line ->
                line.flatMap {
                    when (it) {
                        'O' -> listOf(TileType.BOX_LEFT, TileType.BOX_RIGHT)
                        '@' -> listOf(TileType.ROBOT, TileType.EMPTY)
                        else -> listOf(TileType.fromChar(it), TileType.fromChar(it))
                    }
                }.toMutableList()
            }
        )
    }

    private operator fun get(position: Vec2) = tiles[position.y][position.x]

    private operator fun set(position: Vec2, tileType: TileType) {
        tiles[position.y][position.x] = tileType
    }

    fun tryMoveRobot(direction: Direction) {
        tryMove(robotPosition, direction, checkOnly = false)
    }

    private fun tryMove(
        position: Vec2,
        direction: Direction,
        checkOnly: Boolean
    ): Boolean {
        val tileType = this[position]
        val targetPositions = when {
            tileType == TileType.ROBOT ->
                listOf(position + direction.vector)

            tileType == TileType.BOX_LEFT && direction == Direction.RIGHT ->
                listOf(position + Vec2(1, 0) + direction.vector)

            tileType == TileType.BOX_LEFT ->
                listOf(position + direction.vector, position + Vec2(1, 0) + direction.vector)

            tileType == TileType.BOX_RIGHT && direction == Direction.LEFT ->
                listOf(position + Vec2(-1, 0) + direction.vector)

            tileType == TileType.BOX_RIGHT ->
                listOf(position + direction.vector, position + Vec2(-1, 0) + direction.vector)

            else -> throw IllegalArgumentException("Cannot move tile of type ${this[position]}")
        }

        if (targetPositions.any { this[it] == TileType.WALL }) {
            return false
        }

        if (targetPositions.all { this[it] == TileType.EMPTY }) {
            if (!checkOnly) {
                move(position, direction)
            }
            return true
        }

        if (
            direction in arrayOf(Direction.UP, Direction.DOWN)
            && this[position] in arrayOf(TileType.BOX_LEFT, TileType.BOX_RIGHT)
            && this[position + direction.vector] == this[position]
        ) {
            if (tryMove(position + direction.vector, direction, checkOnly)) {
                if (!checkOnly) {
                    move(position, direction)
                }
                return true
            }
            return false
        }

        if (targetPositions
                .filter { this[it] != TileType.EMPTY }
                .all { tryMove(it, direction, checkOnly = true) }
        ) {
            if (!checkOnly) {
                targetPositions
                    .filter { this[it] != TileType.EMPTY }
                    .forEach { tryMove(it, direction, checkOnly = false) }
                move(position, direction)
            }
            return true
        }
        return false
    }

    private fun move(from: Vec2, direction: Direction) {
        when (val tileType = this[from]) {
            TileType.ROBOT -> {
                this[from + direction.vector] = tileType
                this[from] = TileType.EMPTY
            }

            TileType.BOX_LEFT -> {
                when (direction) {
                    Direction.RIGHT -> {
                        this[from + Vec2(1, 0) + direction.vector] = TileType.BOX_RIGHT
                        this[from + direction.vector] = TileType.BOX_LEFT
                        this[from] = TileType.EMPTY
                    }

                    Direction.LEFT -> {
                        this[from + direction.vector] = TileType.BOX_LEFT
                        this[from + Vec2(1, 0) + direction.vector] = TileType.BOX_RIGHT
                        this[from] = TileType.EMPTY
                    }

                    else -> {
                        this[from + direction.vector] = TileType.BOX_LEFT
                        this[from + Vec2(1, 0) + direction.vector] = TileType.BOX_RIGHT
                        this[from] = TileType.EMPTY
                        this[from + Vec2(1, 0)] = TileType.EMPTY
                    }
                }
            }

            TileType.BOX_RIGHT -> {
                when (direction) {
                    Direction.LEFT -> {
                        this[from + Vec2(-1, 0) + direction.vector] = TileType.BOX_LEFT
                        this[from + direction.vector] = TileType.BOX_RIGHT
                        this[from] = TileType.EMPTY
                    }

                    Direction.RIGHT -> {
                        this[from + direction.vector] = TileType.BOX_RIGHT
                        this[from + Vec2(-1, 0) + direction.vector] = TileType.BOX_LEFT
                        this[from] = TileType.EMPTY
                    }

                    else -> {
                        this[from + direction.vector] = TileType.BOX_RIGHT
                        this[from + Vec2(-1, 0) + direction.vector] = TileType.BOX_LEFT
                        this[from] = TileType.EMPTY
                        this[from + Vec2(-1, 0)] = TileType.EMPTY
                    }
                }
            }

            else -> throw IllegalArgumentException("Tile of type ${this[from]} cannot be moved")
        }
    }

    override fun toString() = tiles
        .joinToString("\n") { row ->
            row.joinToString("") { it.toString() }
        }// + "\n\n$robotPosition"
}

fun main() {
    val input = Path("input.txt").readText()
    val (warehouseInput, movementInput) = input.split("\n\n")
    val warehouse = Warehouse.fromLines(warehouseInput.lines())
    val movements = movementInput
        .filter { it != '\n' }
        .map(Direction::fromChar)

    println(warehouse)

    val states = mutableListOf(warehouse.grid)

    val simulationDuration = measureTime {
        movements.forEach {
//            println(it)
            warehouse.tryMoveRobot(it)
            println(warehouse)
            sleep(5)
            states.add(warehouse.grid)
        }
    }
//    println(warehouse)
    println(simulationDuration)

    val (result, duration) = measureTimedValue { warehouse.sumOfGpsCoordinates }
    println("$result, $duration")
}
