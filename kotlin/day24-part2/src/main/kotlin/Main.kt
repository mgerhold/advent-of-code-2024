package org.example

import kotlin.io.path.Path
import kotlin.io.path.readText

typealias Voltage = Boolean

abstract class Node(val name: String) {
    abstract fun determineOutput(): Voltage

    abstract override fun toString(): String

    abstract fun isEqual(other: Node): Boolean

    open fun isResult(lhs: String, rhs: String, carry: String) = false

    open fun isCarry(lhs: String, rhs: String, carry: String) = false
}

class Input(
    name: String,
    private val voltage: Voltage,
) : Node(name) {
    override fun determineOutput(): Voltage {
        return voltage
    }

    override fun toString(): String {
        return name
    }

    override fun isEqual(other: Node) = other is Input && name == other.name
}

abstract class BinaryOperator(
    val lhs: Node,
    val rhs: Node,
    name: String,
) : Node(name) {
    abstract fun operatorName(): String

    override fun toString(): String {
        return "$name($lhs ${operatorName()} $rhs)"
    }

    override fun isEqual(other: Node) = other is BinaryOperator
            && this::class == other::class
            && (
            (lhs == other.lhs && rhs == other.rhs)
                    || (lhs == other.rhs && rhs == other.lhs)
            )
}

class AndGate(
    lhs: Node,
    rhs: Node,
    name: String,
) : BinaryOperator(lhs, rhs, name) {
    override fun operatorName(): String = "&"

    override fun determineOutput(): Voltage {
        return lhs.determineOutput() && rhs.determineOutput()
    }

    fun hasOperands(a: String, b: String): Boolean {
        return setOf(a, b) == setOf(lhs.name, rhs.name)
    }

    fun hasOperands(xorA: String, xorB: String, other: String): Boolean {
        if (lhs is XorGate && lhs.hasOperands(xorA, xorB) && rhs.name == other) {
            return true
        }
        if (rhs is XorGate && rhs.hasOperands(xorA, xorB) && lhs.name == other) {
            return true
        }
        return false
    }
}

class OrGate(
    lhs: Node,
    rhs: Node,
    name: String,
) : BinaryOperator(lhs, rhs, name) {
    override fun operatorName(): String = "|"

    override fun determineOutput(): Voltage {
        return lhs.determineOutput() || rhs.determineOutput()
    }

    override fun isCarry(lhs: String, rhs: String, carry: String): Boolean {
        if (this.lhs !is AndGate || this.rhs !is AndGate) {
            return false
        }

        if (this.lhs.hasOperands(lhs, rhs) && this.rhs.hasOperands(lhs, rhs, carry)) {
            return true
        }

        if (this.rhs.hasOperands(lhs, rhs) && this.lhs.hasOperands(lhs, rhs, carry)) {
            return true
        }

        return false
    }
}

class XorGate(
    lhs: Node,
    rhs: Node,
    name: String,
) : BinaryOperator(lhs, rhs, name) {
    override fun operatorName(): String = "⊕"

    override fun determineOutput(): Voltage {
        return lhs.determineOutput() != rhs.determineOutput()
    }

    override fun isResult(lhs: String, rhs: String, carry: String): Boolean {
        val needle = setOf(lhs, rhs, carry)
        if (this.lhs is XorGate) {
            val haystack = setOf(this.lhs.lhs.name, this.lhs.rhs.name, this.rhs.name)
            if (needle == haystack) {
                return true
            }
        }
        if (this.rhs is XorGate) {
            val haystack = setOf(this.rhs.lhs.name, this.rhs.rhs.name, this.lhs.name)
            if (needle == haystack) {
                return true
            }
        }
        return false
    }

    fun hasOperands(a: String, b: String): Boolean {
        return setOf(a, b) == setOf(lhs.name, rhs.name)
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

fun Int.toGate(char: Char) = "$char${toString().padStart(2, '0')}"

fun Int.toX() = toGate('x')
fun Int.toY() = toGate('y')
fun Int.toZ() = toGate('z')

fun trySwap(
    inputs: List<Input>,
    parsedGates: List<ParsedGate>,
    a: String,
    b: String
): Map<String, Node>? {
    val swapped = { name: String ->
        when (name) {
            a -> b
            b -> a
            else -> name
        }
    }

    return parsedGates
        .map { gate ->
            ParsedGate(gate.lhs, gate.type, gate.rhs, swapped(gate.out))
        }
        .let { tryBuildGraph(inputs, it) }
}

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

    val gates = parsedGates
        .map{ it.out }

    var max = -1
    for ((a, b) in gates.flatMap { gate -> gates.filter{ it != gate }.map{ gate to it} }) {
        val graph = trySwap(inputs, parsedGates, a, b)
        if (graph == null) {
            continue
        }
        val maxBit = determineMaxWorkingBit(graph)
        if (maxBit > max) {
            max = maxBit
            println("$max ($a <-> $b)")
        }
    }

    /*val graph = tryBuildGraph(inputs, parsedGates)!!
    println(graph.keys.count { !it.startsWith("x") && !it.startsWith("y") })

    println(determineMaxWorkingBit(graph))*/
}

private fun determineMaxWorkingBit(graph: Map<String, Node>): Int {
    var result = graph[0.toZ()]!!
    var carry = graph
        .values
        .filter {
            it is AndGate && (
                    (it.lhs.name == 0.toX() && it.rhs.name == 0.toY())
                            || (it.lhs.name == 0.toY() && it.rhs.name == 0.toX())
                    )
        }
        .also { check(it.count() == 1) }
        .first()

//    println("result of bit  0: $result")
//    println(" carry of bit  0: $carry")

    for (bit in 1 until 45) {
        val lhs = graph[bit.toX()]!!.name
        val rhs = graph[bit.toY()]!!.name
        val newResult = graph
            .values
            .filter { it.isResult(lhs, rhs, carry.name) }
            .firstOrNull()

        if (newResult == null) {
            return bit - 1
        }

        val newCarry = graph
            .values
            .filter { it.isCarry(lhs, rhs, carry.name) }
            .firstOrNull()
        if (newCarry == null) {
            return bit - 1
        }
//        println("result of bit ${bit.toString().padStart(2, ' ')}: $newResult")
//        println(" carry of bit ${bit.toString().padStart(2, ' ')}: $newCarry")

        result = newResult
        carry = newCarry
    }
    return 46
}

private fun tryBuildGraph(
    inputs: List<Input>,
    parsedGates: List<ParsedGate>
): Map<String, Node> {
    val graph = inputs
        .associateBy { node ->
            node.name
        }
        .toMutableMap<String, Node>()

    val gatesToProcess = parsedGates.toMutableList()
    while (gatesToProcess.isNotEmpty()) {
        val gateIndex = gatesToProcess.indexOfFirst {
            it.lhs in graph && it.rhs in graph
        }
        if (gateIndex == -1) {
            break
        }
        val gate = gatesToProcess.removeAt(gateIndex)

        check(gate.out !in graph)
        graph[gate.out] = when (gate.type) {
            GateType.AND -> AndGate(graph[gate.lhs]!!, graph[gate.rhs]!!, gate.out)
            GateType.OR -> OrGate(graph[gate.lhs]!!, graph[gate.rhs]!!, gate.out)
            GateType.XOR -> XorGate(graph[gate.lhs]!!, graph[gate.rhs]!!, gate.out)
        }
    }
    return graph
}
