package org.example

data class Program(
    val registerFile: RegisterFile,
    val instructions: List<Instruction>
) {
    companion object {
        fun fromString(string: String): Program {
            val registerStrings = string.substringBefore("\n\n").lines()
            val registerFile = RegisterFile()
            registerStrings
                .map { it.removePrefix("Register ").split(": ") }
                .map { (name, value) ->
                    when (name) {
                        "A" -> RegisterIdentifier.A
                        "B" -> RegisterIdentifier.B
                        "C" -> RegisterIdentifier.C
                        else -> throw IllegalArgumentException("Invalid register identifier '$name'.")
                    } to value.toULong()
                }
                .forEach { (identifier, value) ->
                    registerFile[identifier] = value
                }

            val instructions = string
                .substringAfter("\n\n")
                .trim()
                .removePrefix("Program: ")
                .split(",")
                .map { it.toUByte() }
                .chunked(2)
                .map { (opcode, operand) -> Instruction.fromBytes(opcode, operand) }

            return Program(registerFile, instructions)
        }
    }
}
