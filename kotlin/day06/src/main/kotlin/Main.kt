package org.example

import kotlin.io.path.Path
import kotlin.io.path.readLines

typealias Room = List<List<Tile>>

data class Vec2(val x: Int, val y: Int) {
    operator fun plus(other: Vec2): Vec2 = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2): Vec2 = Vec2(x - other.x, y - other.y)
    operator fun unaryMinus(): Vec2 = Vec2(-x, -y)

    fun rotatedClockwise() = Vec2(-y, x)

    companion object {
        fun up() = Vec2(0, -1)
    }
}

data class Guard(val position: Vec2, val direction: Vec2) {
    fun walkedForward() = Guard(position + direction, direction)
    fun rotated() = Guard(position, direction.rotatedClockwise())
}

enum class Tile {
    EMPTY,
    OBSTACLE,
}

fun charToTile(char: Char) = when (char) {
    '.' -> Tile.EMPTY
    '^' -> Tile.EMPTY
    '#' -> Tile.OBSTACLE
    else -> throw IllegalArgumentException("'$char' is not a valid map tile.")
}

fun parseRoom(lines: List<String>) = lines.map { it.toList().map(::charToTile) }

fun getGuard(lines: List<String>) = lines
    .withIndex()
    .filter { (_, row) -> '^' in row }
    .map { (rowIndex, row) -> Vec2(row.indexOf('^'), rowIndex) }
    .first()
    .let { position -> Guard(position, Vec2.up()) }

fun isInsideRoom(position: Vec2, room: Room) = position.x in room[0].indices && position.y in room.indices

fun canWalkForward(guard: Guard, room: Room): Boolean {
    val nextPosition = guard.position + guard.direction
    return !isInsideRoom(nextPosition, room) || room[nextPosition.y][nextPosition.x] == Tile.EMPTY
}

fun getDirectionSymbol(direction: Vec2) = when (direction) {
    Vec2.up() -> '^'
    Vec2(1, 0) -> '>'
    Vec2(-1 ,0) -> '<'
    Vec2(0, 1) -> 'v'
    else -> throw IllegalArgumentException("Unexpected direction vector.")
}

fun visualize(guard: Guard, room: Room) {
    room.forEachIndexed { rowIndex, row ->
        row.forEachIndexed { columnIndex, tile ->
            val position = Vec2(columnIndex, rowIndex)
            if (position == guard.position) {
                print(getDirectionSymbol(guard.direction))
            } else {
                print(
                    when (tile) {
                        Tile.EMPTY -> '.'
                        Tile.OBSTACLE -> '#'
                    }
                )
            }
        }
        println()
    }
}

fun getVisitedPositions(guard: Guard, room: Room): Set<Vec2> {
    @Suppress("NAME_SHADOWING")
    var guard = guard
    val visitedPositions = mutableSetOf(guard.position)
    while (isInsideRoom(guard.position, room)) {
        if (canWalkForward(guard, room)) {
            guard = guard.walkedForward()
            visitedPositions.add(guard.position)
        } else {
            guard = guard.rotated()
        }
    }
    return visitedPositions
}

fun countVisitedPositions(guard: Guard, room: Room) = getVisitedPositions(guard, room).size - 1

fun hasLoop(guard: Guard, room: Room): Boolean {
    @Suppress("NAME_SHADOWING")
    var guard = guard
    val previousStates = mutableSetOf(guard)
    while (true) {
        if (!isInsideRoom(guard.position, room)) {
            return false
        }
        if (canWalkForward(guard, room)) {
            guard = guard.walkedForward()
            if (guard in previousStates) {
                return true
            }
            previousStates.add(guard)
        } else {
            guard = guard.rotated()
        }
    }
}

fun countObstaclePositionsProducingLoops(guard: Guard, initialRoom: Room): Int {
    val visitedPositions = getVisitedPositions(guard, initialRoom)
        .subtract(setOf(guard.position))
        .filter { isInsideRoom(it, initialRoom) }
    val room = initialRoom.map { it.toMutableList() }
    var numLoops = 0
    for (position in visitedPositions) {
        room[position.y][position.x] = Tile.OBSTACLE
        if (hasLoop(guard, room)) {
            ++numLoops
        }
        room[position.y][position.x] = Tile.EMPTY
    }
    return numLoops
}

fun main() {
    val lines = Path("input.txt")
        .readLines()

    val room = parseRoom(lines)
    val guard = getGuard(lines)
    println(countVisitedPositions(guard, room))
    println(countObstaclePositionsProducingLoops(guard, room))
}
