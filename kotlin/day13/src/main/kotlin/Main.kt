package org.example

import kotlin.io.path.Path
import kotlin.io.path.readLines
import kotlin.math.abs

data class Vec2(val x: Long, val y: Long) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
}

data class ClawMachine(val a: Vec2, val b: Vec2, val target: Vec2)

fun parseButton(line: String) = line
    .substringAfter(": ")
    .split(", ")
    .map { it.substringAfter("+").toLong() }
    .let { (x, y) -> Vec2(x, y) }

fun parsePrize(line: String) = line
    .substringAfter(": ")
    .split(", ")
    .map { it.substringAfter("=").toLong() }
    .let { (x, y) -> Vec2(x, y) }

fun findPrize(clawMachine: ClawMachine): Vec2? {
    // ax * r + bx * s = tx  | * ay
    // ay * r + by * s = ty  | * ax

    // ax * ay * r + ay * bx * s = ay * tx
    // ax * ay * r + ax * by * s = ax * ty

    // (ay * bx - ax * by) * s = ay * tx - ax * ty

    val (a, b, t) = clawMachine
    val coefficient = a.y * b.x - a.x * b.y
    val resultS = a.y * t.x - a.x * t.y

    if (coefficient == 0L) {
        check(resultS != 0L)
        return null
    }

    if (resultS % coefficient != 0L) {
        return null
    }

    val s = resultS / coefficient
    // ax * r + bx * s = tx
    // ax * r = tx - bx * s
    val resultR = t.x - b.x * s
    if (resultR % a.x != 0L) {
        return null
    }
    val r = resultR / a.x
    return Vec2(r, s)
}

fun main() {
    val clawMachines = Path("input.txt")
        .readLines()
        .chunked(4)
        .map { (lineA, lineB, linePrize, _) ->
            ClawMachine(
                parseButton(lineA),
                parseButton(lineB),
                parsePrize(linePrize)
            )
        }

    println("Part 1")
    clawMachines
        .asSequence()
        .map(::findPrize)
        .withIndex()
        .onEach { (i, linearCombination) ->
            print("Claw machine #${i + 1}: ")
            if (linearCombination == null) {
                println("<no solution found>")
            } else {
                println("press A ${linearCombination.x} times and B ${linearCombination.y} times")
            }
        }
        .mapNotNull { (_, linearCombination) -> linearCombination }
        .sumOf { (a, b) -> 3L * a + b }
        .also { println("Total number of tokens required: $it") }

    println("Part 2")
    val part2offset = Vec2(10000000000000L, 10000000000000L)
    clawMachines
        .asSequence()
        .map { ClawMachine(it.a, it.b, it.target + part2offset) }
        .map(::findPrize)
        .withIndex()
        .onEach { (i, linearCombination) ->
            print("Claw machine #${i + 1}: ")
            if (linearCombination == null) {
                println("<no solution found>")
            } else {
                println("press A ${linearCombination.x} times and B ${linearCombination.y} times")
            }
        }
        .mapNotNull { (_, linearCombination) -> linearCombination }
        .sumOf { (a, b) -> 3L * a + b }
        .also { println("Total number of tokens required: $it") }
}
