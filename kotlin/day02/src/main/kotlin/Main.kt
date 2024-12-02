package org.example

import kotlin.io.path.Path
import kotlin.io.path.readLines
import kotlin.math.abs

fun isReportSafe(levels: Sequence<Int>): Boolean {
    require(levels.count() >= 2)
    val deltas = levels
        .windowed(2)
        .map { (current, next) -> next - current }
    val firstDelta = deltas.first()
    return deltas.all { (abs(it) in 1..3) && (it >= 0 == firstDelta >= 0) }
}

fun canReportBeMadeSafe(levels: Sequence<Int>): Boolean {
    return (0 until levels.count())
        .any { i -> levels.filterIndexed { index, _ -> index != i }.let(::isReportSafe) }
}

fun main() {
    val lines = Path("input.txt").readLines()
    listOf(::isReportSafe, ::canReportBeMadeSafe)
        .forEachIndexed { index, validator ->
            lines
                .count { it.split(" ").map(String::toInt).asSequence().let(validator) }
                .let { "Part ${index + 1}: $it" }
                .also(::println)
        }
}
