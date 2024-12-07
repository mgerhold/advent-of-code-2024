package org.example

import kotlin.io.path.Path
import kotlin.io.path.readLines

enum class Part1Operator {
    PLUS,
    MULTIPLY,
}

enum class Part2Operator {
    PLUS,
    MULTIPLY,
    CONCATENATE,
}

fun concatenate(a: ULong, b: ULong): ULong {
    if (b == 0UL) {
        return a
    }
    var number = b
    var base = 1UL
    while (number > 0UL) {
        base *= 10UL
        number /= 10UL
    }
    return a * base + b
}

data class Equation(val testValue: ULong, val operands: List<ULong>) {
    fun isSolvablePart1(): Boolean {
        return Part1Operator
            .entries
            .any { isSolvable(operands.first(), 0, it) }
    }

    fun isSolvablePart2(): Boolean {
        return Part2Operator
            .entries
            .any { isSolvable(operands.first(), 0, it) }
    }

    private fun isSolvable(
        partialResult: ULong,
        operatorIndex: Int,
        operator: Part1Operator,
    ): Boolean {
        val tooManyOperands = operatorIndex > operands.size - 2
        if (tooManyOperands) {
            return partialResult == testValue
        }

        val operand = operands[operatorIndex + 1]
        val nextPartialResult = when (operator) {
            Part1Operator.PLUS -> partialResult + operand
            Part1Operator.MULTIPLY -> partialResult * operand
        }

        return Part1Operator
            .entries
            .any {
                isSolvable(
                    partialResult = nextPartialResult,
                    operatorIndex = operatorIndex + 1,
                    operator = it,
                )
            }
    }

    private fun isSolvable(
        partialResult: ULong,
        operatorIndex: Int,
        operator: Part2Operator,
    ): Boolean {
        val tooManyOperands = operatorIndex > operands.size - 2
        if (tooManyOperands) {
            return partialResult == testValue
        }

        val operand = operands[operatorIndex + 1]
        val nextPartialResult = when (operator) {
            Part2Operator.PLUS -> partialResult + operand
            Part2Operator.MULTIPLY -> partialResult * operand
            Part2Operator.CONCATENATE -> concatenate(partialResult, operand)
        }

        return Part2Operator
            .entries
            .any {
                isSolvable(
                    partialResult = nextPartialResult,
                    operatorIndex = operatorIndex + 1,
                    operator = it,
                )
            }
    }

    companion object {
        fun parse(line: String): Equation {
            val testValue = line.substringBefore(':').toULong()
            val operands = line
                .substringAfter(": ")
                .split(" ")
                .map(String::toULong)
            return Equation(testValue, operands)
        }
    }
}

fun main() {
    val equations = Path("input.txt").readLines().map(Equation::parse)
    equations
        .filter { it.isSolvablePart1() }
        .sumOf { it.testValue }
        .also { println("Part 1: $it") }

    equations
        .filter { it.isSolvablePart2() }
        .sumOf { it.testValue }
        .also { println("Part 2: $it") }
}
