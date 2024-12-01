package org.example

import java.io.File
import kotlin.math.abs

fun main() {
    // Part 1.
    val lines = File("input.txt").readLines()
    val lists = lines
        .map { it.split("   ") }
        .map { it[0].toInt() to it[1].toInt() }
        .unzip()
    val (first, second) = lists.first.sorted() to lists.second.sorted()
    val result = first
        .zip(second)
        .sumOf { abs(it.first - it.second) }
    println(result)

    // Part 2.
    val counts = mutableMapOf<Int, Int>()
    for (number in lists.second) {
        counts[number] = counts.getOrDefault(number, 0) + 1
    }
    println(lists.first.sumOf { it * counts.getOrDefault(it, 0) })
}
