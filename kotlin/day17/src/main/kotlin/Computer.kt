package org.example

class Computer(program: Program) {
    private val instructions = program.instructions
    private val registerFile = program.registerFile.copy()
    private var instructionPointer = 0
    private val outputBuffer: MutableList<UByte> = mutableListOf()

    var halted: Boolean = false
        private set

    val output: String
        get() = outputBuffer
            .joinToString(",") { it.toString() }

    fun executeNextInstruction() {
        if (halted) {
            return
        }

        if (instructionPointer !in instructions.indices) {
            halted = true
            return
        }

        val instruction = instructions[instructionPointer]
        execute(instruction)
    }

    fun printState() {
        Terminal.hideCursor()
        Terminal.clearScreen()
        registerFile
            .forEach { (name, value) ->
                Terminal.setForegroundColor(AnsiColor.BRIGHT_WHITE)
                print(name)
                Terminal.setForegroundColor(AnsiColor.WHITE)
                print(value)
                println()
            }

        println()

        instructions
            .forEachIndexed { i, instruction ->
                if (i == instructionPointer) {
                    Terminal.setForegroundColor(AnsiColor.BRIGHT_WHITE)
                    println(" >$instruction")
                } else {
                    Terminal.setForegroundColor(AnsiColor.WHITE)
                    println("  $instruction")
                }
            }
    }

    private fun execute(instruction: Instruction) {
        when (instruction) {
            is Instruction.Adv -> {
                val lhs = registerFile[RegisterIdentifier.A]
                val rhs = (1 shl instruction.operand.fetchValue(registerFile).toInt()).toUInt()
                registerFile[RegisterIdentifier.A] = lhs / rhs
                ++instructionPointer
            }

            is Instruction.Bxl -> {
                val lhs = registerFile[RegisterIdentifier.B]
                val rhs = instruction.operand.value
                registerFile[RegisterIdentifier.B] = lhs xor rhs.toULong()
                ++instructionPointer
            }

            is Instruction.Bst -> {
                val lhs = instruction.operand.fetchValue(registerFile)
                registerFile[RegisterIdentifier.B] = lhs % 8UL
                ++instructionPointer
            }

            is Instruction.Jnz -> {
                val a = registerFile[RegisterIdentifier.A]
                if (a.toInt() == 0) {
                    ++instructionPointer
                    return
                }

                val operand = instruction.operand.value
                instructionPointer = operand.toInt()
            }

            is Instruction.Bxc -> {
                val lhs = registerFile[RegisterIdentifier.B]
                val rhs = registerFile[RegisterIdentifier.C]
                registerFile[RegisterIdentifier.B] = lhs xor rhs
                ++instructionPointer
            }

            is Instruction.Out -> {
                outputBuffer.add((instruction.operand.fetchValue(registerFile) % 8UL).toUByte())
                ++instructionPointer
            }

            is Instruction.Bdv -> {
                val lhs = registerFile[RegisterIdentifier.A]
                val rhs = (1 shl instruction.operand.fetchValue(registerFile).toInt()).toUInt()
                registerFile[RegisterIdentifier.B] = lhs / rhs
                ++instructionPointer
            }

            is Instruction.Cdv -> {
                val lhs = registerFile[RegisterIdentifier.A]
                val rhs = (1 shl instruction.operand.fetchValue(registerFile).toInt()).toUInt()
                registerFile[RegisterIdentifier.C] = lhs / rhs
                ++instructionPointer
            }
        }
    }
}
