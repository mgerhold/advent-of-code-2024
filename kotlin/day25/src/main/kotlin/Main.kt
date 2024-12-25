package org.example

import kotlin.io.path.Path
import kotlin.io.path.readText

const val NUM_COLUMNS = 5
const val NUM_ROWS = 7
const val MAX_HEIGHT = NUM_ROWS - 2

sealed class Parsable(val heights: List<Int>) {
    class Lock(pinHeights: List<Int>) : Parsable(pinHeights)
    class Key(shapeHeights: List<Int>) : Parsable(shapeHeights)

    companion object {
        fun parse(string: String): Parsable {
            return (0 until NUM_COLUMNS)
                .map { columnIndex ->
                    (0 until NUM_ROWS)
                        .map { rowIndex ->
                            string[(NUM_COLUMNS + 1) * rowIndex + columnIndex]
                        }
                        .count { it == '#' }
                        .dec()
                }
                .let { if (string.startsWith('#')) Lock(it) else Key(it) }
        }
    }

    override fun toString() = heights.joinToString(",")
}

sealed class Node {
    class Inner(val children: List<Node>) : Node() {
        override fun toString() = "(${children.joinToString(",")})"
    }

    class Leaf(val accumulatedHeightCount: Int) : Node() {
        override fun toString() = accumulatedHeightCount.toString()
    }

    fun traverse(heights: List<Int>) = heights
        .fold(this) { current, height ->
            (current as Inner).children[height]
        }
}

fun buildTree(locks: List<Parsable.Lock>, startingColumn: Int): Node {
    return if (startingColumn == NUM_COLUMNS - 1) {
        (0..MAX_HEIGHT)
            .map { upperHeightBound ->
                locks.count { it.heights[startingColumn] <= upperHeightBound }
            }
            .map { accumulatedHeightCount ->
                Node.Leaf(accumulatedHeightCount)
            }
            .let(Node::Inner)
    } else {
        (0..MAX_HEIGHT)
            .map { upperHeightBound ->
                locks.filter { lock -> lock.heights[startingColumn] <= upperHeightBound }
            }
            .map { locksWithHeight -> buildTree(locksWithHeight, startingColumn + 1) }
            .let(Node::Inner)
    }
}

fun main() {
    Path("input.txt")
        .readText()
        .trim()
        .split("\n\n")
        .map(Parsable::parse)
        .let { parsables ->
            parsables.filterIsInstance<Parsable.Lock>() to parsables.filterIsInstance<Parsable.Key>()
        }
        .let { (locks, keys) -> buildTree(locks, 0) to keys }
        .let { (tree, keys) ->
            keys
                .sumOf { key ->
                    tree
                        .traverse(key.heights.map { MAX_HEIGHT - it })
                        .let { it as Node.Leaf }
                        .accumulatedHeightCount
                }
        }
        .also { total -> println("total: $total") }
}
