package org.example

import java.util.PriorityQueue
import kotlin.io.path.Path
import kotlin.io.path.readLines
import kotlin.math.max
import kotlin.math.min

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
    acceptedTileTypes: Array<TileType>,
) = arrayOf(
    Vec2(1, 0), Vec2(-1, 0), Vec2(0, 1), Vec2(0, -1)
).map { offset -> position + offset }
    .filter { it.x in grid.first().indices && it.y in grid.indices }
    .map { grid[it.y][it.x] }
    .filter { it.type in acceptedTileTypes }

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
    ignoreWalls: Boolean,
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
            acceptedTileTypes = when (ignoreWalls) {
                true -> TileType.entries.toTypedArray()
                false -> TileType.entries.filter { it != TileType.WALL }.toTypedArray()
            }
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

    dijkstra(grid, startTilePosition, Long.MAX_VALUE, ignoreWalls = false)
    visualize(grid, endTilePosition)
    println()

    val path = reconstructPath(grid, endTilePosition).map { it.position }

    val resetGrid = {
        grid
            .forEach { row ->
                row
                    .forEach { tile ->
                        tile.cost = Long.MAX_VALUE
                    }
            }
    }

    path
        .flatMapIndexed { i, position ->
            println("Dijkstra ${i + 1} of ${path.size}...")
            System.out.flush()
            resetGrid()

            val visited = dijkstra(
                grid,
                position,
                min(20L, (path.size - i - 1).toLong()),
                ignoreWalls = true
            )
            visited
                .asSequence()
                .filter { tile -> tile.type != TileType.WALL }
                .map { tile ->
                    tile to path.indexOf(tile.position).toLong() - (i + tile.cost)
                }
                .filter { (tile, saving) -> saving > 0L }
        }
        .groupingBy { (tile, saving) -> saving }
        .eachCount()
        .entries
        .filter { (saving, count) -> saving >= 100 }
        .sortedBy { (saving, count) -> saving }
        .onEach { (saving, count) -> println("There are $count cheats that save $saving picoseconds.") }
        .sumOf { (saving, count) -> count }
        .also(::println)
}
