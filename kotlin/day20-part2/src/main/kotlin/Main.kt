package org.example

import java.util.PriorityQueue
import kotlin.io.path.Path
import kotlin.io.path.readLines
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.time.measureTimedValue

const val MAX_CHEAT_DISTANCE = 20

enum class TileType(val char: Char) {
    WALL('#'),
    TRACK('.'),
    START('S'),
    END('E'),
}

data class Vec2(val x: Int, val y: Int) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(scalar: Int) = Vec2(x * scalar, y * scalar)
}

operator fun Int.times(vec: Vec2) = vec * this

class Tile(
    val position: Vec2,
    val type: TileType,
    var predecessor: Tile?,
    var cost: Long,
) {
    override fun toString(): String {
        if (predecessor == null) {
            return "Tile($position, $type)"
        }
        return "Tile($position, $type, $cost)"
    }
}

typealias Grid = List<List<Tile>>

fun findTileByType(grid: Grid, tileType: TileType) = grid.flatMapIndexed { rowIndex, row ->
    row.mapIndexed { columnIndex, tile ->
        Vec2(columnIndex, rowIndex) to tile
    }
}.first { (_, tile) -> tile.type == tileType }.first

fun findStartPosition(grid: Grid) = findTileByType(grid, TileType.START)

fun findEndPosition(grid: Grid) = findTileByType(grid, TileType.END)

fun findNeighbors(
    grid: Grid,
    position: Vec2,
) = arrayOf(
    Vec2(1, 0), Vec2(-1, 0), Vec2(0, 1), Vec2(0, -1)
).map { offset -> position + offset }
    .filter { it.x in grid.first().indices && it.y in grid.indices }
    .map { grid[it.y][it.x] }
    .filter { it.type != TileType.WALL }

fun reconstructPath(grid: Grid, endTilePosition: Vec2): List<Tile> {
    val endTile = grid[endTilePosition.y][endTilePosition.x]

    var current: Tile? = endTile
    val path = mutableListOf<Tile>()
    while (current != null) {
        path.add(current)
        current = current.predecessor
    }
    path.reverse()
    return path
}

fun dijkstra(
    grid: Grid,
    startPosition: Vec2,
    maxCost: Long,
): Set<Tile> {
    val visited = mutableSetOf<Tile>()
    val toVisit = PriorityQueue<Tile>(compareBy { it.cost })
    grid[startPosition.y][startPosition.x].cost = 0L
    toVisit.add(grid[startPosition.y][startPosition.x])

    while (toVisit.isNotEmpty()) {
        val tile = toVisit.poll()
        visited.add(tile)
        if (tile.cost == maxCost) {
            continue
        }

        val neighbors = findNeighbors(
            grid,
            tile.position,
        )
        val tentativeCost = tile.cost + 1L
        for (neighbor in neighbors) {
            if (tentativeCost < neighbor.cost) {
                neighbor.cost = tentativeCost
                neighbor.predecessor = tile
                toVisit.add(neighbor)
            }
        }
    }
    return visited
}

fun visualize(grid: Grid, endTilePosition: Vec2) {
    val path = reconstructPath(grid, endTilePosition).map { it.position }
    grid
        .forEachIndexed { y, row ->
            row
                .forEachIndexed { x, tile ->
                    val pathIndex = path.indexOf(Vec2(x, y))
                    if (pathIndex == -1 || tile.type in arrayOf(TileType.START, TileType.END)) {
                        print(grid[y][x].type.char)
                    } else {
                        print('O')
                    }
                }
            println()
        }
}

fun main() {
    val grid = Path("input.txt").readLines().mapIndexed { rowIndex, row ->
        row.mapIndexed { columnIndex, c ->
            Vec2(columnIndex, rowIndex) to when (c) {
                '#' -> TileType.WALL
                '.' -> TileType.TRACK
                'S' -> TileType.START
                'E' -> TileType.END
                else -> throw IllegalArgumentException("Unexpected tile: '$c'")
            }
        }.map { (position, tileType) ->
            Tile(position, tileType, null, Long.MAX_VALUE)
        }
    }

    val startTilePosition = findStartPosition(grid)
    val endTilePosition = findEndPosition(grid)

    dijkstra(grid, startTilePosition, Long.MAX_VALUE)
    visualize(grid, endTilePosition)
    println()

    val path = reconstructPath(grid, endTilePosition).map { it.position }

    val pathMapping = path
        .withIndex()
        .associate { (i, position) -> position to i }

    repeat(30) {
        measureTimedValue {
            path
                .asSequence()
                .flatMapIndexed { i, position ->
                    val reachableTiles =
                        (max(0, position.y - MAX_CHEAT_DISTANCE)..min(grid.size - 1, position.y + MAX_CHEAT_DISTANCE))
                            .asSequence()
                            .flatMap { y ->
                                val maxXOffset = MAX_CHEAT_DISTANCE - abs(y - position.y)
                                (max(0, position.x - maxXOffset)..min(grid.first().size - 1, position.x + maxXOffset))
                                    .asSequence()
                                    .map { x -> Vec2(x, y) }
                            }
                            .filter { grid[it.y][it.x].type != TileType.WALL }

                    reachableTiles
                        .filter { pathMapping[it]!! > i }
                        .map { otherPosition ->
                            otherPosition to abs(otherPosition.x - position.x) + abs(otherPosition.y - position.y)
                        }
                        .map { (otherPosition, manhattanDistance) ->
                            pathMapping[otherPosition]!! - i - manhattanDistance
                        }
                        .filter { it >= 100 }
                }
                .count()
        }.also(::println)
    }
}
