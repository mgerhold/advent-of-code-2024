package org.example

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

    // Gap left of Key 0.
    KEY_0(Vec2(1, 3), '0'),
    KEY_A(Vec2(2, 3), 'A'),
}

enum class DirectionalKey(val position: Vec2, val char: Char, val direction: Vec2) {
    /*     +---+---+
           | ^ | A |
       +---+---+---+
       | < | v | > |
       +---+---+---+ */
    UP(Vec2(1, 0), '^', Vec2(0, -1)),
    A(Vec2(2, 0), 'A', Vec2(0, 0)),
    LEFT(Vec2(0, 1), '<', Vec2(-1, 0)),
    DOWN(Vec2(1, 1), 'v', Vec2(0, 1)),
    RIGHT(Vec2(2, 1), '>', Vec2(1, 0)),
}

fun toNumpadSequence(string: String) = string
    .map { char ->
        NumpadKey.entries.first { key -> key.char == char }
    }

fun numpadToDirectional(sequence: List<NumpadKey>): List<DirectionalKey> {
    val result = mutableListOf<DirectionalKey>()
    var lastKey = NumpadKey.KEY_A
    var lastDirection: DirectionalKey? = null
    for (targetKey in sequence) {
        val delta = targetKey.position - lastKey.position
        val movingUp = delta.y < 0
        val movingLeft = delta.x < 0

        val moveVertically = {
            (0 until abs(delta.y))
                .forEach { _ ->
                    result.add(if (movingUp) DirectionalKey.UP else DirectionalKey.DOWN)
                }
        }
        val moveHorizontally = {
            (0 until abs(delta.x))
                .forEach { _ ->
                    result.add(if (movingLeft) DirectionalKey.LEFT else DirectionalKey.RIGHT)
                }
        }

        if (movingUp) {
            moveVertically()
            moveHorizontally()
        } else {
            moveHorizontally()
            moveVertically()
        }

        lastKey = targetKey
        result.add(DirectionalKey.A)
    }
    return result
}

fun directionalToDirectional(sequence: List<DirectionalKey>): List<DirectionalKey> {
    val result = mutableListOf<DirectionalKey>()
    var lastKey = DirectionalKey.A
    for (targetKey in sequence) {
        val delta = targetKey.position - lastKey.position
        val movingUp = delta.y < 0
        val movingLeft = delta.x < 0

        val moveVertically = {
            (0 until abs(delta.y))
                .forEach { _ ->
                    result.add(if (movingUp) DirectionalKey.UP else DirectionalKey.DOWN)
                }
        }
        val moveHorizontally = {
            (0 until abs(delta.x))
                .forEach { _ ->
                    result.add(if (movingLeft) DirectionalKey.LEFT else DirectionalKey.RIGHT)
                }
        }

        if (movingUp) {
            moveHorizontally()
            moveVertically()
        } else {
            moveVertically()
            moveHorizontally()
        }

        lastKey = targetKey
        result.add(DirectionalKey.A)
    }
    return result
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

fun main() {
    /*Path("input.txt")
        .readLines()
        .asSequence()
        .map { it.substring(0, 3).toInt() to toNumpadSequence(it) }
        .onEach { (_, sequence) ->
            sequence
                .map { key -> key.char }
                .joinToString("")
                .also(::println)
        }
        .map { (numericalPart, sequence) -> numericalPart to numpadToDirectional(sequence) }
        .onEach { (_, sequence) ->
            sequence
                .map { key -> key.char }
                .joinToString("")
                .also(::println)
            val reversed = reverseNumericalToDirectional(sequence)
            reversed
                .map { key -> key.char }
                .joinToString("")
                .also { println("  reversed: $it") }
        }
        .map { (numericalPart, sequence) -> numericalPart to directionalToDirectional(sequence) }
        .onEach { (_, sequence) ->
            sequence
                .map { key -> key.char }
                .joinToString("")
                .also(::println)
            val reversed = reverseDirectionalToDirectional(sequence)
            reversed
                .map { key -> key.char }
                .joinToString("")
                .also { println("  reversed: $it") }
        }
        .map { (numericalPart, sequence) -> numericalPart to directionalToDirectional(sequence) }
        .onEach { (_, sequence) ->
            sequence
                .map { key -> key.char }
                .joinToString("")
                .also(::println)
            val reversed = reverseDirectionalToDirectional(sequence)
            reversed
                .map { key -> key.char }
                .joinToString("")
                .also { println("  reversed: $it") }
        }
        .forEach { (numericalPart, sequence) ->
            println("${sequence.size} * $numericalPart = ${sequence.size * numericalPart}")
        }*/
}
