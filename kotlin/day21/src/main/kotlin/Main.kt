package org.example

import kotlin.io.path.Path
import kotlin.io.path.readLines
import kotlin.math.abs

data class Vec2(val x: Int, val y: Int) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
}

enum class NumpadKey(val position: Vec2, val char: Char) {
    /* +---+---+---+
       | 7 | 8 | 9 |
       +---+---+---+
       | 4 | 5 | 6 |
       +---+---+---+
       | 1 | 2 | 3 |
       +---+---+---+
           | 0 | A |
           +---+---+ */
    KEY_7(Vec2(0, 0), '7'),
    KEY_8(Vec2(1, 0), '8'),
    KEY_9(Vec2(2, 0), '9'),
    KEY_4(Vec2(0, 1), '4'),
    KEY_5(Vec2(1, 1), '5'),
    KEY_6(Vec2(2, 1), '6'),
    KEY_1(Vec2(0, 2), '1'),
    KEY_2(Vec2(1, 2), '2'),
    KEY_3(Vec2(2, 2), '3'),
    KEY_FORBIDDEN(Vec2(0, 3), ' '),
    KEY_0(Vec2(1, 3), '0'),
    KEY_A(Vec2(2, 3), 'A');

    override fun toString(): String {
        return char.toString()
    }
}

enum class DirectionalKey(val position: Vec2, val char: Char, val direction: Vec2) {
    /*     +---+---+
           | ^ | A |
       +---+---+---+
       | < | v | > |
       +---+---+---+ */
    FORBIDDEN(Vec2(0, 0), ' ', Vec2(0, 0)),
    UP(Vec2(1, 0), '^', Vec2(0, -1)),
    A(Vec2(2, 0), 'A', Vec2(0, 0)),
    LEFT(Vec2(0, 1), '<', Vec2(-1, 0)),
    DOWN(Vec2(1, 1), 'v', Vec2(0, 1)),
    RIGHT(Vec2(2, 1), '>', Vec2(1, 0));

    override fun toString(): String {
        return char.toString()
    }
}

fun toNumpadSequence(string: String) = string
    .map { char ->
        NumpadKey.entries.first { key -> key.char == char }
    }

fun isValidNumpadSequence(sequence: List<DirectionalKey>): Boolean {
    var current = NumpadKey.KEY_A
    for (directionKey in sequence) {
        check(directionKey != DirectionalKey.FORBIDDEN)
        current = NumpadKey.entries.first { it.position == current.position + directionKey.direction }
        if (current == NumpadKey.KEY_FORBIDDEN) {
            return false
        }
    }
    return true
}

fun isValiDirectionalSequence(sequence: List<DirectionalKey>): Boolean {
    var current = DirectionalKey.A
    for (directionKey in sequence) {
        check(directionKey != DirectionalKey.FORBIDDEN)
        current = DirectionalKey.entries.first { it.position == current.position + directionKey.direction }
        if (current == DirectionalKey.FORBIDDEN) {
            return false
        }
    }
    return true
}

fun numpadToDirectional(sequence: List<NumpadKey>): Sequence<List<DirectionalKey>> {
    val blocks = mutableListOf<List<DirectionalKey>>()
    var lastKey = NumpadKey.KEY_A
    for (targetKey in sequence) {
        val subSequence = mutableListOf<DirectionalKey>()
        val delta = targetKey.position - lastKey.position

        (0 until abs(delta.y))
            .forEach { _ ->
                subSequence.add(if (delta.y < 0) DirectionalKey.UP else DirectionalKey.DOWN)
            }

        (0 until abs(delta.x))
            .forEach { _ ->
                subSequence.add(if (delta.x < 0) DirectionalKey.LEFT else DirectionalKey.RIGHT)
            }

        lastKey = targetKey
        subSequence.add(DirectionalKey.A)
        blocks.add(subSequence)
    }

    var result: Sequence<List<DirectionalKey>>? = null
    for (block in blocks) {
        val permutations = getPermutations(block.subList(0, block.size - 1)).map { it + block.last() }
        if (result == null) {
            result = permutations
        } else {
            result = result
                .flatMap { a ->
                    permutations
                        .map { b ->
                            a + b
                        }
                }
        }
    }
    check(result != null)
    return result.filter(::isValidNumpadSequence)
}

fun getPermutations(sequence: List<DirectionalKey>): Sequence<List<DirectionalKey>> {
    if (sequence.size <= 1) {
        return sequenceOf(sequence)
    }

    val subPermutations = getPermutations(sequence.subList(0, sequence.size - 1))
    return (0..subPermutations.first().size)
        .asSequence()
        .flatMap { i ->
            subPermutations
                .map {
                    val permutation = it.toMutableList()
                    permutation.add(i, sequence.last())
                    permutation
                }
        }
        .distinct()
}

fun directionalToDirectional(sequence: List<DirectionalKey>): Sequence<List<DirectionalKey>> {
    val blocks = mutableListOf<List<DirectionalKey>>()
    var lastKey = DirectionalKey.A
    for (targetKey in sequence) {
        val subSequence = mutableListOf<DirectionalKey>()
        val delta = targetKey.position - lastKey.position

        (0 until abs(delta.y))
            .forEach { _ ->
                subSequence.add(if (delta.y < 0) DirectionalKey.UP else DirectionalKey.DOWN)
            }

        (0 until abs(delta.x))
            .forEach { _ ->
                subSequence.add(if (delta.x < 0) DirectionalKey.LEFT else DirectionalKey.RIGHT)
            }

        lastKey = targetKey
        subSequence.add(DirectionalKey.A)
        blocks.add(subSequence)
    }

    var result: Sequence<List<DirectionalKey>>? = null
    for (block in blocks) {
        val permutations = getPermutations(block.subList(0, block.size - 1)).map { it + block.last() }
        if (result == null) {
            result = permutations
        } else {
            result = result
                .flatMap { a ->
                    permutations
                        .map { b ->
                            a + b
                        }
                }
        }
    }
    check(result != null)
    return result.filter(::isValiDirectionalSequence)
}

fun reverseDirectionalToDirectional(sequence: List<DirectionalKey>): List<DirectionalKey> {
    var currentKey = DirectionalKey.A
    val result = mutableListOf<DirectionalKey>()
    for (inputKey in sequence) {
        if (inputKey == DirectionalKey.A) {
            result.add(currentKey)
            continue
        }
        val delta = when (inputKey) {
            DirectionalKey.UP -> Vec2(0, -1)
            DirectionalKey.LEFT -> Vec2(-1, 0)
            DirectionalKey.DOWN -> Vec2(0, 1)
            DirectionalKey.RIGHT -> Vec2(1, 0)
            else -> throw RuntimeException("Unreachable")
        }
        currentKey = DirectionalKey.entries.first { it.position == currentKey.position + delta }
    }
    return result
}

fun reverseNumericalToDirectional(sequence: List<DirectionalKey>): List<NumpadKey> {
    var currentKey = NumpadKey.KEY_A
    val result = mutableListOf<NumpadKey>()
    for (inputKey in sequence) {
        if (inputKey == DirectionalKey.A) {
            result.add(currentKey)
            continue
        }
        val delta = when (inputKey) {
            DirectionalKey.UP -> Vec2(0, -1)
            DirectionalKey.LEFT -> Vec2(-1, 0)
            DirectionalKey.DOWN -> Vec2(0, 1)
            DirectionalKey.RIGHT -> Vec2(1, 0)
            else -> throw RuntimeException("Unreachable")
        }
        currentKey = NumpadKey.entries.first { it.position == currentKey.position + delta }
    }
    return result
}

fun findShortestLength(sequences: Sequence<List<DirectionalKey>>, depth: Int): Int {
    if (depth == 0) {
        return sequences.first().size
    }
    return sequences
        .minOf { sequence ->
            findShortestLength(directionalToDirectional(sequence), depth - 1)
        }
}

fun main() {
    val sequences = Path("input.txt")
        .readLines()
        .filter { !it.startsWith("//") }
        .map { it.substring(0, it.length - 1).toInt() to toNumpadSequence(it) }

    var sum = 0
    for ((numericValue, sequence) in sequences) {
        val directionSequences1 = numpadToDirectional(sequence)
        val shortestLength = findShortestLength(directionSequences1, 2)
        println("$shortestLength * $numericValue = ${shortestLength * numericValue}")
        sum += shortestLength * numericValue
    }
    println("sum: $sum")
}
