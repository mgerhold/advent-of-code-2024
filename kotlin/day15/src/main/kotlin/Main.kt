package org.example

import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

enum class TileType(private val char: Char) {
    WALL('#'),
    BOX('O'),
    ROBOT('@'),
    EMPTY('.');

    companion object {
        fun fromChar(char: Char) = TileType
            .entries
            .find { it.char == char } ?: throw IllegalArgumentException("Invalid map char: '$char'")
    }

    override fun toString() = char.toString()
}

enum class Direction(private val char: Char, public val vector: Vec2) {
    NORTH('^', Vec2(0, -1)),
    EAST('>', Vec2(1, 0)),
    SOUTH('v', Vec2(0, 1)),
    WEST('<', Vec2(-1, 0));

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
                    .filter { (_, tileType) -> tileType == TileType.BOX }
            }
            .map { it.first }
            .sumOf { it.x + 100 * it.y }

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
            lines.map { line -> line.map(TileType::fromChar).toMutableList() }
        )
    }

    private operator fun get(position: Vec2) = tiles[position.y][position.x]

    private operator fun set(position: Vec2, tileType: TileType) {
        tiles[position.y][position.x] = tileType
    }

    fun tryMoveRobot(direction: Direction) {
        tryMove(robotPosition, direction)
    }

    private fun tryMove(position: Vec2, direction: Direction): Boolean {
        val targetPosition = position + direction.vector
        if (this[targetPosition] == TileType.WALL) {
            return false
        }
        val movementResult = this[targetPosition] == TileType.EMPTY || tryMove(targetPosition, direction)
        if (movementResult) {
            move(position, targetPosition)
        }
        return movementResult
    }

    private fun move(from: Vec2, to: Vec2) {
        this[to] = this[from]
        this[from] = TileType.EMPTY
    }

    override fun toString() = tiles
        .joinToString("\n") { row ->
            row.joinToString("") { it.toString() }
        } + "\n\n$robotPosition"
}

fun main() {
    val input = Path("input.txt").readText()
    val (warehouseInput, movementInput) = input.split("\n\n")
    val warehouse = Warehouse.fromLines(warehouseInput.lines())
    val movements = movementInput
        .filter { it != '\n' }
        .map(Direction::fromChar)

    val simulationDuration = measureTime {
        movements.forEach { warehouse.tryMoveRobot(it) }
    }
    println(warehouse)
    println(simulationDuration)

    val (result, duration) = measureTimedValue { warehouse.sumOfGpsCoordinates }
    println("$result, $duration")
}
