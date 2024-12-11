package org.example

import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.time.measureTime

val lookupTable = generateSequence(10UL) { 10UL * it }.take(10).toList()

fun blink(stones: Map<ULong, ULong>) = stones
    .flatMap { (value, count) ->
        // Special case for 0.
        if (value == 0UL) {
            return@flatMap sequenceOf(1UL to count)
        }

        var base = 10UL
        for (entry in lookupTable) {
            if (value < base) {
                return@flatMap sequenceOf(value * 2024UL to count)
            }
            base *= 10UL
            if (value < base) {
                return@flatMap sequenceOf(value / entry to count, value % entry to count)
            }
            base *= 10UL
        }
        // More than 19 digits.
        return@flatMap sequenceOf(value / 10_000_000_000UL to count, value % 10_000_000_000UL to count)
    }
    .groupingBy { (value, _) -> value }
    .fold(0UL) { accumulator, pair -> accumulator + pair.second }

fun main() {
    val input = Path("input.txt")
        .readText()
        .trim()
        .split(" ")
        .map(String::toULong)

    val stoneCounts = input
        .groupingBy { it }
        .fold(0UL) { accumulator, _ -> accumulator + 1UL }

    arrayOf(25, 75)
        .forEachIndexed { repetitionIndex, numRepetitions ->
            print("Part ${repetitionIndex + 1}: ")
            val duration = measureTime {
                (0..<numRepetitions)
                    .fold(stoneCounts) { stones, _ -> blink(stones) }
                    .values
                    .sum()
                    .also(::print)
            }
            println(" ($duration)")
        }
}
