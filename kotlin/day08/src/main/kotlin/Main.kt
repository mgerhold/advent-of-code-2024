package org.example

import kotlin.io.path.Path
import kotlin.io.path.readLines

typealias Frequency = Char

sealed class Tile {
    object Empty : Tile() {}
    class Antenna(val frequency: Frequency) : Tile() {}

    companion object {
        fun fromChar(char: Char): Tile {
            return when (char) {
                '.' -> Empty
                '#' -> Empty
                else -> Antenna(char)
            }
        }
    }

    fun toChar(): Char {
        return when (this) {
            Empty -> '.'
            is Antenna -> this.frequency
        }
    }

    override fun toString(): String {
        return toChar().toString()
    }
}

data class Vec2(val x: Int, val y: Int) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(scalar: Int) = Vec2(x * scalar, y * scalar)
}

class TileMap private constructor(private val tiles: List<List<Tile>>) : Iterable<Pair<Vec2, Tile>> {
    companion object {
        fun fromLines(lines: List<String>): TileMap {
            return TileMap(
                lines
                    .map { it.map { char -> Tile.fromChar(char) } }
            )
        }
    }

    override fun toString(): String {
        return tiles
            .joinToString("\n") {
                it
                    .map { tile -> tile.toChar() }
                    .joinToString("")
            }
    }

    operator fun get(position: Vec2): Tile {
        return tiles[position.y][position.x]
    }

    operator fun contains(position: Vec2): Boolean {
        return position.y in tiles.indices && position.x in tiles[0].indices
    }

    override operator fun iterator(): Iterator<Pair<Vec2, Tile>> {
        return tiles
            .indices
            .asSequence()
            .flatMap { rowIndex ->
                tiles[rowIndex]
                    .indices
                    .asSequence()
                    .map { Vec2(it, rowIndex) }
            }
            .map { it to get(it) }
            .iterator()
    }
}

fun <T> getCombinations(elements: Sequence<T>) = elements
    .withIndex()
    .flatMap { (i, first) ->
        elements
            .withIndex()
            .drop(i + 1)
            .map { (_, second) -> first to second }
    }

fun getAntinodes(antennaPositions: Pair<Vec2, Vec2>): Set<Vec2> {
    val (a, b) = antennaPositions
    return setOf(a + (b - a) * 2, a - (b - a))
}

fun getAntinodesPart2(antennaPositions: Pair<Vec2, Vec2>): Pair<Sequence<Vec2>, Sequence<Vec2>> {
    val (a, b) = antennaPositions
    return generateSequence(a) { it + (b - a) } to generateSequence(a - (b - a)) { it - (b - a) }
}

fun main() {
    val tileMap = Path("input.txt")
        .readLines()
        .let { TileMap.fromLines(it) }

    val antennasByFrequency: Map<Char, List<Vec2>> = tileMap
        .filter { (_, tile) -> tile is Tile.Antenna }
        .groupingBy { (_, tile) -> (tile as Tile.Antenna).frequency }
        .aggregate { _, set: MutableList<Vec2>?, element, _ ->
            set?.apply { this.add(element.first) } ?: mutableListOf(element.first)
        }

    antennasByFrequency
        .flatMap { (_, positions) ->
            getCombinations(positions.asSequence())
                .flatMap(::getAntinodes)
                .filter { it in tileMap }
        }
        .distinct()
        .count()
        .also { println("Part 1: $it") }

    antennasByFrequency
        .flatMap { (_, positions) ->
            getCombinations(positions.asSequence())
                .map(::getAntinodesPart2)
                .flatMap { (a, b) ->
                    a.takeWhile { it in tileMap } + b.takeWhile { it in tileMap }
                }
        }
        .distinct()
        .count()
        .also { println("Part 2: $it") }
}
