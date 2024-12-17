package org.example

sealed class Operand {
    class Literal(val value: UByte) : Operand() {
        override fun toString(): String {
            return "Literal($value)"
        }
    }

    class Combo(private val value: UByte) : Operand() {
        init {
            if (value !in 0U..6U) {
                throw IllegalArgumentException("Cannot create combo operand from byte value '$value'.")
            }
        }

        fun fetchValue(registerFile: RegisterFile): ULong {
            require(value.toUInt() != 7U)
            if (value in 0U..3U) {
                return value.toULong()
            }
            val registerIdentifier = RegisterIdentifier.entries[value.toInt() - 4]
            return registerFile[registerIdentifier]
        }

        override fun toString(): String {
            require(value.toUInt() != 7U)
            if (value in 0U..3U) {
                return "Combo($value)"
            }
            return when (value.toInt()) {
                4 -> "Combo(A)"
                5 -> "Combo(B)"
                6 -> "Combo(C)"
                else -> throw RuntimeException("Unreachable")
            }
        }
    }
}
