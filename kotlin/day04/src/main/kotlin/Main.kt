package org.example

import kotlin.io.path.Path
import kotlin.io.path.readLines

fun countOccurrences(needle: List<Char>, haystack: List<List<Char>>): Int {
    var numMatches = 0
    for ((rowIndex, row) in haystack.withIndex()) {
        var needleNextIndex = 0
        for ((columnIndex, c) in row.withIndex()) {
            if (needle[needleNextIndex] != c) {
                needleNextIndex = 0
            }
            if (needle[needleNextIndex] == c) {
                ++needleNextIndex
                if (needleNextIndex == needle.size) {
                    ++numMatches
                    needleNextIndex = 0
                }
            }
        }
    }
    return numMatches
}

fun part1(data: List<List<Char>>): Int {
    val needle = "XMAS".toList()
    val haystacks = listOf(
        // Horizontal.
        data,
        // Vertical.
        (0..<data[0].size).map { columnIndex ->
            data.indices.map { rowIndex -> data[rowIndex][columnIndex] }.toList()
        }.toList(),
        // Diagonals from top left to bottom right.
        // 1. Diagonals below (and including) the middle diagonal.
        data
            .indices
            .map { rowIndex -> rowIndex to (data.size - rowIndex) }
            .map { (rowIndex, diagonalLength) -> (0..<diagonalLength).map { data[rowIndex + it][it] }.toList() }
            .toList(),
        // 2. Diagonals above the middle diagonal.
        (1..<data[0].size)
            .map { columnIndex -> columnIndex to (data[0].size - columnIndex) }
            .map { (columnIndex, diagonalLength) -> (0..<diagonalLength).map { data[it][columnIndex + it] }.toList() }
            .toList(),
        // Diagonals from top right to bottom left.
        // 1. Diagonals below (and including) the middle diagonal.
        data
            .indices
            .map { rowIndex -> rowIndex to (data.size - rowIndex) }
            .map { (rowIndex, diagonalLength) ->
                (0..<diagonalLength).map { data[rowIndex + it][data[0].size - it - 1] }.toList()
            }
            .toList(),
        // 2. Diagonals above the middle diagonal.
        (1..<data[0].size)
            .map { columnIndex -> columnIndex to (data[0].size - columnIndex) }
            .map { (columnIndex, diagonalLength) ->
                (0..<diagonalLength).map { data[it][data[0].size - 1 - (columnIndex + it)] }.toList()
            }
            .toList(),
    )
    return haystacks
        .map { it to it.map { list -> list.reversed() }.toList() }
        .sumOf { pair ->
            pair.toList().sumOf {
                countOccurrences(needle, it)
            }
        }
}

fun checkOppositeChars(a: Char, b: Char): Boolean {
    return when (a to b) {
        ('M' to 'S') -> true
        ('S' to 'M') -> true
        else -> false
    }
}

fun part2(data: List<List<Char>>): Int {
    val isXMasPattern: (Pair<Int, Int>) -> Boolean = { (rowIndex, columnIndex) ->
        data[rowIndex][columnIndex] == 'A'
                && checkOppositeChars(data[rowIndex - 1][columnIndex - 1], data[rowIndex + 1][columnIndex + 1])
                && checkOppositeChars(data[rowIndex - 1][columnIndex + 1], data[rowIndex + 1][columnIndex - 1])
    }

    return data
        .withIndex()
        .drop(1)
        .dropLast(1)
        .asSequence()
        .map { (rowIndex, row) -> rowIndex to row.indices.drop(1).dropLast(1).asSequence() }
        .flatMap { (rowIndex, columnIndices) -> columnIndices.map { rowIndex to it } }
        .filter(isXMasPattern)
        .count()
}

fun main() {
    val data = Path("input.txt").readLines().map(String::toList)
    println(part1(data))
    println(part2(data))
}
