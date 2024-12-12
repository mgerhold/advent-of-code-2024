package org.example

import kotlin.io.path.Path
import kotlin.io.path.readLines
import kotlin.time.measureTimedValue

data class Point(val x: Int, val y: Int) {
    operator fun plus(other: Point) = Point(x + other.x, y + other.y)
    operator fun minus(other: Point) = Point(x - other.x, y - other.y)
}

typealias Plant = Char

fun calculatePerimeter(plots: Set<Point>): ULong {
    val getNumNeighbors = { position: Point ->
        arrayOf(
            Point(position.x - 1, position.y),
            Point(position.x + 1, position.y),
            Point(position.x, position.y - 1),
            Point(position.x, position.y + 1),
        )
            .count { it in plots }
            .toULong()
    }

    return plots.sumOf { 4UL - getNumNeighbors(it) }
}

fun determineRegions(garden: MutableMap<Point, Plant>): List<Pair<Plant, Set<Point>>> {
    val width = garden.keys.maxByOrNull { (x, _) -> x }!!.x + 1
    val height = garden.keys.maxByOrNull { (_, y) -> y }!!.y + 1

    val isWithinGarden = { position: Point -> position.x in 0..<width && position.y in 0..<height }

    val getAdjacent = { position: Point ->
        arrayOf(
            Point(position.x - 1, position.y),
            Point(position.x + 1, position.y),
            Point(position.x, position.y - 1),
            Point(position.x, position.y + 1),
        )
            .filter(isWithinGarden)
            .toSet()
    }

    val regions = mutableListOf<Pair<Plant, Set<Point>>>()

    for (rowIndex in 0..<height) {
        for (columnIndex in 0..<width) {
            val position = Point(columnIndex, rowIndex)
            val plant = garden[position] ?: continue

            val region = mutableSetOf(position)
            val visited = mutableSetOf(position)
            val toVisit = getAdjacent(position).let(::ArrayDeque)

            while (toVisit.isNotEmpty()) {
                val positionToCheck = toVisit.removeFirst()
                visited.add(positionToCheck)
                val plantToCheck = garden[positionToCheck] ?: continue
                if (plantToCheck != plant) {
                    continue
                }
                garden.remove(positionToCheck)
                region.add(positionToCheck)
                getAdjacent(positionToCheck)
                    .subtract(visited)
                    .apply { toVisit.addAll(this) }
            }
            regions.add(plant to region)
        }
    }

    return regions
}

fun part1(regions: List<Pair<Plant, Set<Point>>>): ULong {
    return regions
        .sumOf { (_, plots) -> plots.size.toULong() * calculatePerimeter(plots) }
}

typealias Edge = Pair<Point, Point>

fun determineEdges(region: Set<Point>): Set<Edge> {
    val edges = mutableSetOf<Edge>()
    for (plot in region) {
        // Right side.
        if (Point(plot.x + 1, plot.y) !in region) {
            edges.add(Point(plot.x + 1, plot.y) to Point(plot.x + 1, plot.y + 1))
        }
        // Left side.
        if (Point(plot.x - 1, plot.y) !in region) {
            edges.add(Point(plot.x, plot.y + 1) to Point(plot.x, plot.y))
        }
        // Top side.
        if (Point(plot.x, plot.y - 1) !in region) {
            edges.add(Point(plot.x, plot.y) to Point(plot.x + 1, plot.y))
        }
        // Bottom side.
        if (Point(plot.x, plot.y + 1) !in region) {
            edges.add(Point(plot.x + 1, plot.y + 1) to Point(plot.x, plot.y + 1))
        }
    }
    return edges
}

fun collapseEdges(edges: Set<Edge>): Set<Edge> {
    val discarded = mutableSetOf<Edge>()
    val collapsed = mutableSetOf<Edge>()
    for (edge in edges) {
        if (edge in discarded) {
            continue
        }
        collapsed.add(edge)
        val (start, end) = edge
        val delta = end - start

        var current = end to end + delta
        while (current in edges) {
            discarded.add(current)
            current = current.first + delta to current.second + delta
        }
        current = start - delta to start
        while (current in edges) {
            discarded.add(current)
            current = current.first - delta to current.second - delta
        }
    }
    return collapsed
}

fun part2(regions: List<Pair<Plant, Set<Point>>>): ULong {
    return regions
        .sumOf { (_, plots) ->
            plots.size.toULong() * plots.let(::determineEdges).let(::collapseEdges).size.toULong()
        }
}

fun main() {
    val (input, readLinesDuration) = measureTimedValue { Path("input.txt").readLines() }
    val (garden, parseToMapDuration) = measureTimedValue {
        input
            .withIndex()
            .flatMap { (rowIndex, row) ->
                row.withIndex().map { (columnIndex, plant) -> Point(columnIndex, rowIndex) to plant }
            }
            .toMap()
    }
    val (regions, determineRegionsDuration) = measureTimedValue {
        determineRegions(garden.toMutableMap())
    }

    val (part1solution, part1duration) = measureTimedValue { part1(regions) }
    val (part2solution, part2duration) = measureTimedValue { part2(regions) }
    println(part1solution)
    println(part2solution)

    println("read input into list of strings: $readLinesDuration")
    println("parse input into a Map<Point, Plant>: $parseToMapDuration")
    println("determine regions: $determineRegionsDuration")
    println("part 1: $part1duration")
    println("part 2: $part2duration")
}
