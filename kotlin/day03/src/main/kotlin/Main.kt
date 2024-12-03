package org.example

import kotlin.io.path.Path
import kotlin.io.path.readText

fun part1(text: String): Int {
    val regex = """mul\((?<lhs>\d{1,3}),(?<rhs>\d{1,3})\)""".toRegex()
    return regex
        .findAll(text)
        .map { it.groups["lhs"]!!.value.toInt() * it.groups["rhs"]!!.value.toInt() }
        .sum()
}

fun part2(text: String): Int {
    val regex = """(?<mul>mul)\((?<lhs>\d{1,3}),(?<rhs>\d{1,3})\)|(?<enable>do)\(\)|(?<disable>don't)\(\)""".toRegex()
    val matches = regex.findAll(text)
    var mulEnabled = true
    var sum = 0
    for (match in matches) {
        if (match.groups["enable"] != null) {
            mulEnabled = true
            continue
        }

        if (match.groups["disable"] != null) {
            mulEnabled = false
            continue
        }

        if (!mulEnabled) {
            continue
        }

        check(match.groups["mul"] != null)
        sum += match.groups["lhs"]!!.value.toInt() * match.groups["rhs"]!!.value.toInt()
    }
    return sum
}

fun main() {
    val text = Path("input.txt").readText()
    println(part1(text))
    println(part2(text))
}
