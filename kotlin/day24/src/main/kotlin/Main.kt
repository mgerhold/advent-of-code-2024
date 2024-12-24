package org.example

import kotlin.io.path.Path
import kotlin.io.path.readText

typealias Voltage = Boolean

abstract class Node(val name: String) {
    abstract fun determineOutput(): Voltage
}

class Input(
    name: String,
    private val voltage: Voltage,
) : Node(name) {
    override fun determineOutput(): Voltage {
        return voltage
    }
}

class AndGate(
    private val lhs: Node,
    private val rhs: Node,
    name: String,
) : Node(name) {
    override fun determineOutput(): Voltage {
        return lhs.determineOutput() && rhs.determineOutput()
    }
}

class OrGate(
    private val lhs: Node,
    private val rhs: Node,
    name: String,
) : Node(name) {
    override fun determineOutput(): Voltage {
        return lhs.determineOutput() || rhs.determineOutput()
    }
}

class XorGate(
    private val lhs: Node,
    private val rhs: Node,
    name: String,
) : Node(name) {
    override fun determineOutput(): Voltage {
        return lhs.determineOutput() != rhs.determineOutput()
    }
}

enum class GateType {
    AND,
    OR,
    XOR,
}

data class ParsedGate(
    val lhs: String,
    val type: GateType,
    val rhs: String,
    val out: String,
)

fun main() {
    val inputText = Path("input.txt").readText()

    val inputs = inputText
        .substringBefore("\n\n")
        .lines()
        .map { it.split(": ") }
        .map { (name, initialValue) ->
            Input(name, initialValue == "1")
        }

    val parsedGates = inputText
        .trim()
        .substringAfter("\n\n")
        .lines()
        .map { line -> line.split(" -> ") }
        .map { (signature, out) ->
            val (lhs, operator, rhs) = signature.split(" ")
            ParsedGate(
                lhs,
                GateType.valueOf(operator),
                rhs,
                out,
            )
        }

    val nodes = inputs
        .associateBy { node ->
            node.name
        }
        .toMutableMap<String, Node>()

    val gatesToProcess = parsedGates.toMutableList()
    while (gatesToProcess.isNotEmpty()) {
        val gateIndex = gatesToProcess.indexOfFirst {
            it.lhs in nodes && it.rhs in nodes
        }
        val gate = gatesToProcess.removeAt(gateIndex)

        check(gate.out !in nodes)
        nodes[gate.out] = when (gate.type) {
            GateType.AND -> AndGate(nodes[gate.lhs]!!, nodes[gate.rhs]!!, gate.out)
            GateType.OR -> OrGate(nodes[gate.lhs]!!, nodes[gate.rhs]!!, gate.out)
            GateType.XOR -> XorGate(nodes[gate.lhs]!!, nodes[gate.rhs]!!, gate.out)
        }
    }

    nodes
        .entries
        .filter { (key, value) -> key.startsWith("z") }
        .sortedBy { (key, value) -> key }
        .map { (key, value) -> value.determineOutput() }
        .map { voltage -> if (voltage) 1UL else 0UL }
        .reversed()
        .also { print("0b") }
        .onEach(::print)
        .also { println() }
        .fold(0UL) { accumulator, bit ->
            (accumulator shl 1) or bit
        }
        .also(::println)
}
