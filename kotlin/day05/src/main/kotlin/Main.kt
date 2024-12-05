package org.example

import kotlin.io.path.Path
import kotlin.io.path.readLines

typealias Update = List<Int>
typealias Ordering = Map<Int, Set<Int>>
typealias SemiPartition = Set<Int>
typealias Partition = Pair<SemiPartition, SemiPartition>

fun checkSemiPartition(
    page: Int,
    update: Update,
    ordering: Ordering,
    semiPartition: SemiPartition
): Boolean {
    return page !in ordering || ordering[page]!!.intersect(update).all { it in semiPartition }
}

fun isPageValid(
    page: Int,
    update: Update,
    partition: Partition,
    pagesThatComeAfter: Ordering,
    pagesThatComeBefore: Ordering
): Boolean {
    // formatter:off
    return (
        checkSemiPartition(
            page,
            update,
            pagesThatComeAfter,
            partition.second
        )
        && checkSemiPartition(
            page,
            update,
            pagesThatComeBefore,
            partition.first
        )
    )
    // formatter:on
}

fun isUpdateOrderedCorrectly(
    update: Update,
    pagesThatComeAfter: Ordering,
    pagesThatComeBefore: Ordering
): Boolean {
    val partitions = update
        .indices
        .map { update.take(it).toSet() to update.drop(it + 1).toSet() }

    val isPageValid: (Pair<Int, Partition>) -> Boolean = { (page, partition) ->
        isPageValid(page, update, partition, pagesThatComeAfter, pagesThatComeBefore)
    }

    return update
        .asSequence()
        .zip(partitions.asSequence())
        .map(isPageValid)
        .all { it }
}

fun comparePages(
    lhs: Int,
    rhs: Int,
    pagesThatComeAfter: Ordering,
    pagesThatComeBefore: Ordering
): Int {
    if (lhs in pagesThatComeAfter && pagesThatComeAfter[lhs]?.contains(rhs) == true) {
        return -1
    }
    if (lhs in pagesThatComeBefore && pagesThatComeBefore[lhs]?.contains(rhs) == true) {
        return 1
    }
    return 0
}

fun applyOrdering(
    update: Update,
    pagesThatComeAfter: Ordering,
    pagesThatComeBefore: Ordering
): Update {
    return update
        .sortedWith { lhs, rhs ->
            comparePages(lhs, rhs, pagesThatComeAfter, pagesThatComeBefore)
        }
}

fun main() {
    val lines = Path("input.txt").readLines()
    val mappings = lines
        .takeWhile { it.isNotEmpty() }
        .map { it.split("|") }
        .map { it[0].toInt() to it[1].toInt() }
    val pagesThatComeAfter = mappings
        .groupBy { it.first }
        .map { (key, pairs) -> key to pairs.map { it.second }.toSet() }
        .toMap()
    val pagesThatComeBefore = mappings
        .map { (first, second) -> second to first }
        .groupBy { it.first }
        .map { (key, pairs) -> key to pairs.map { it.second }.toSet() }
        .toMap()

    val updates = lines
        .dropWhile { it.isNotEmpty() }
        .dropWhile { it.isEmpty() }
        .map { line -> line.split(",").map { it.toInt() } }

    // Part 1
    updates
        .filter { isUpdateOrderedCorrectly(it, pagesThatComeAfter, pagesThatComeBefore) }
        .sumOf { it[it.size / 2] }
        .also(::println)

    // Part 2
    updates
        .filter { !isUpdateOrderedCorrectly(it, pagesThatComeAfter, pagesThatComeBefore) }
        .map { applyOrdering(it, pagesThatComeAfter, pagesThatComeBefore) }
        .sumOf { it[it.size / 2] }
        .also(::println)
}
