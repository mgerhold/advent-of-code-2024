package org.example

data class RegisterFile(
    private val registers: MutableMap<RegisterIdentifier, ULong> = RegisterIdentifier
        .entries
        .associateWith { 0UL }
        .toMutableMap()
) : Iterable<Map.Entry<RegisterIdentifier, ULong>> {
    operator fun get(identifier: RegisterIdentifier) = registers[identifier]!!

    operator fun set(identifier: RegisterIdentifier, value: ULong) {
        registers[identifier] = value
    }

    override fun iterator() = registers.toMap().iterator()

    fun copy(): RegisterFile {
        return RegisterFile(registers.toMutableMap())
    }
}
