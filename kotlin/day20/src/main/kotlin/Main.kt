package org.example

import java.util.PriorityQueue
import kotlin.io.path.Path
import kotlin.io.path.readLines

enum class TileType(val char: Char) {
    WALL('#'),
    TRACK('.'),
    START('S'),
    END('E'),
}

data class Vec2(val x: Int, val y: Int) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun times(scalar: Int) = Vec2(x * scalar, y * scalar)
}

operator fun Int.times(vec: Vec2) = vec * this

class Tile(
    val position: Vec2,
    val type: TileType,
    var predecessor: Tile?,
    var cost: ULong,
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

fun findNeighbors(grid: Grid, position: Vec2) = arrayOf(
    Vec2(1, 0), Vec2(-1, 0), Vec2(0, 1), Vec2(0, -1)
).map { offset -> position + offset }
    .filter { it.x in grid.first().indices && it.y in grid.indices }
    .map { grid[it.y][it.x] }
    .filter { it.type != TileType.WALL }

fun reconstructPath(grid: Grid): List<Tile> {
    val endTilePosition = findEndPosition(grid)
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

fun dijkstra(grid: Grid) {
    val startPosition = findStartPosition(grid)
    val visited = mutableSetOf<Tile>()
    val toVisit = PriorityQueue<Tile>(compareBy { it.cost })
    grid[startPosition.y][startPosition.x].cost = 0UL
    toVisit.add(grid[startPosition.y][startPosition.x])

    while (toVisit.isNotEmpty()) {
        val tile = toVisit.poll()
        visited.add(tile)

        val neighbors = findNeighbors(grid, tile.position)
        for (neighbor in neighbors) {
            val tentativeCost = tile.cost + 1UL
            if (tentativeCost < neighbor.cost) {
                neighbor.cost = tentativeCost
                neighbor.predecessor = tile
                toVisit.add(neighbor)
            }
        }
    }
}

fun visualize(grid: Grid) {
    val path = reconstructPath(grid).map { it.position }
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

fun part1(path: List<Vec2>, grid: Grid) {
    val offsets = arrayOf(Vec2(1, 0), Vec2(-1, 0), Vec2(0, 1), Vec2(0, -1))
    val savings = path
        .flatMapIndexed { i, position ->
            offsets
                .filter { offset ->
                    val adjacent = position + offset
                    grid[adjacent.y][adjacent.x].type == TileType.WALL
                }
                .map { offset ->
                    val jumpTarget = position + 2 * offset
                    path.indexOf(jumpTarget)
                }
                .filter { it > i }
                .map { position to it - i - 2 }
        }
        .groupingBy { (_, saving) -> saving }
        .eachCount()

    savings
        .entries
        .asSequence()
        .sortedBy { (saving, _) -> saving }
        .filter { (saving, _) -> saving >= 100 }
        .onEach { (saving, count) ->
            println("There are $count cheats that save $saving picoseconds.")
        }
        .sumOf { (_, count) -> count }
        .also(::println)
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
            Tile(position, tileType, null, ULong.MAX_VALUE)
        }
    }

    dijkstra(grid)
    visualize(grid)

    val path = reconstructPath(grid).map { it.position }
    part1(path, grid)
}
