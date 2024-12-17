package org.example

import java.lang.Thread.sleep
import kotlin.io.path.Path
import kotlin.io.path.readText

fun getProgramResult(program: Program, visualize: Boolean = false): String {
    val computer = Computer(program)
    if (visualize) {
        computer.printState()
    }
    while (!computer.halted) {
        computer.executeNextInstruction()
        if (visualize) {
            computer.printState()
            sleep(50)
        }
    }
    if (visualize) {
        Terminal.setForegroundColor(AnsiColor.WHITE)
        println("=== HALTED ===")
        Terminal.showCursor()
    }
    return computer.output
}

fun main() {
    val program = Path("input.txt")
        .readText()
        .let(Program::fromString)
        .also(::println)

    // Part 1.
    println("Output: ${getProgramResult(program, visualize = true)}")

    val expectedResult = Path("input.txt")
        .readText()
        .substringAfter("\n\n")
        .removePrefix("Program: ")
        .trim()

    println(expectedResult)

    val sequence = sequence {
        var value = 290600348058UL
        yield(value)
        while (true) {
            for (delta in arrayOf(786432UL, 245760UL, 16384UL, 49152UL, 98304UL, 68718280704UL, 786432UL, 245760UL, 16384UL, 49152UL, 98304UL, 1030790955008UL, 786432UL, 245760UL, 16384UL, 49152UL, 98304UL, 68718280704UL, 786432UL, 245760UL, 16384UL, 49152UL, 98304UL, 347891154944UL, 786432UL, 245760UL, 16384UL, 49152UL, 98304UL, 68718280704UL, 786432UL, 245760UL, 16384UL, 49152UL, 98304UL, 614179127296UL, 786432UL, 245760UL, 16384UL, 49152UL, 98304UL, 68718280704UL, 786432UL, 245760UL, 16384UL, 49152UL, 98304UL, 1030790955008UL, 786432UL, 245760UL, 16384UL, 49152UL, 98304UL, 68718280704UL, 786432UL, 245760UL, 16384UL, 49152UL, 98304UL, 1030790955008UL, 786432UL, 245760UL, 16384UL, 49152UL, 98304UL, 68718280704UL, 786432UL, 245760UL, 16384UL, 49152UL, 98304UL, 1030790955008UL, 786432UL, 245760UL, 16384UL, 49152UL, 98304UL, 68718280704UL, 786432UL, 245760UL, 16384UL, 49152UL, 98304UL, 1030790955008UL, 786432UL, 245760UL, 16384UL, 49152UL, 98304UL, 68718280704UL, 786432UL, 245760UL, 16384UL, 49152UL, 98304UL, 1030790955008UL, 786432UL, 245760UL, 16384UL, 49152UL, 98304UL, 68718280704UL, 786432UL, 245760UL, 16384UL, 49152UL, 98304UL, 1030790955008UL)) {
                value += delta
                yield(value)
            }
        }
    }

    val iterator = sequence.iterator()
    var last: ULong? = null
    while (true) {
        val value = iterator.next()
        program.registerFile[RegisterIdentifier.A] = value
        val computer = Computer(program)
        repeat(16) {
            repeat(program.instructions.size) {
                computer.executeNextInstruction()
            }
        }
        if (computer.output != "2,4,1,5,7,5,1,6,0,3,4,0,5,5,3,0") {
            continue
        }
        if (last == null) {
            println("${value}UL")
            break
        }
        if (last != null) {
            val delta = value - last
            print(", ${delta}UL")
        }
        last = value
    }
}
