package org.example

sealed class Instruction {
    data class Adv(val operand: Operand.Combo) : Instruction() {
        // adv(combo): division of register A and 2 to the power of the operand, stores result in register A
        companion object {
            fun opcode() = 0.toUByte()
        }
    }

    data class Bxl(val operand: Operand.Literal) : Instruction() {
        // bxl(literal): bitwise XOR of register B and operand, store result in B
        companion object {
            fun opcode() = 1.toUByte()
        }
    }

    data class Bst(val operand: Operand.Combo) : Instruction() {
        // bst(combo): operand MODULO 8, writes result into register B
        companion object {
            fun opcode() = 2.toUByte()
        }
    }

    data class Jnz(val operand: Operand.Literal) : Instruction() {
        // jnz(literal): nothing if A == 0, otherwise sets instruction pointer to operand
        companion object {
            fun opcode() = 3.toUByte()
        }
    }

    class Bxc : Instruction() {
        // bxc(ignored): bitwise XOR of registers B and C, stores result in B
        companion object {
            fun opcode() = 4.toUByte()
        }

        override fun toString(): String {
            return "Bxc"
        }
    }

    data class Out(val operand: Operand.Combo) : Instruction() {
        // out(combo): operand MODULO 8, outputs result (all results separated by commas)
        companion object {
            fun opcode() = 5.toUByte()
        }
    }

    data class Bdv(val operand: Operand.Combo) : Instruction() {
        // bdv(combo): division of register A and 2 to the power of the operand, stores result in register B
        companion object {
            fun opcode() = 6.toUByte()
        }
    }

    data class Cdv(val operand: Operand.Combo) : Instruction() {
        // cdv((combo): division of register A and 2 to the power of the operand, stores result in register C
        companion object {
            fun opcode() = 7.toUByte()
        }
    }

    companion object {
        fun fromBytes(instructionByte: UByte, operandByte: UByte): Instruction {
            return when (instructionByte) {
                Adv.opcode() -> Adv(Operand.Combo(operandByte))
                Bxl.opcode() -> Bxl(Operand.Literal(operandByte))
                Bst.opcode() -> Bst(Operand.Combo(operandByte))
                Jnz.opcode() -> Jnz(Operand.Literal(operandByte))
                Bxc.opcode() -> Bxc()
                Out.opcode() -> Out(Operand.Combo(operandByte))
                Bdv.opcode() -> Bdv(Operand.Combo(operandByte))
                Cdv.opcode() -> Cdv(Operand.Combo(operandByte))
                else -> throw IllegalArgumentException("Unknown opcode $instructionByte.")
            }
        }
    }
}
