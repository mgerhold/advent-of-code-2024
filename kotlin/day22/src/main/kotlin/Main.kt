package org.example

import kotlin.io.path.*
import kotlin.time.measureTimedValue

val PERIOD = (1U shl 24) - 1U

fun nextSecretNumber(secretNumber: UInt): UInt {
    val base = 1U shl 24

    @Suppress("NAME_SHADOWING")
    var secretNumber = secretNumber

    // Calculate the result of multiplying the secret number by 64. Then, mix this result
    // into the secret number. Finally, prune the secret number.
    secretNumber = secretNumber xor (secretNumber * 64U)
    secretNumber %= base

    // Calculate the result of dividing the secret number by 32. Round the result down to
    // the nearest integer. Then, mix this result into the secret number. Finally, prune the
    // secret number.
    secretNumber = secretNumber xor (secretNumber / 32U)
    secretNumber %= base

    // Calculate the result of multiplying the secret number by 2048. Then, mix this result
    // into the secret number. Finally, prune the secret number.
    secretNumber = secretNumber xor (secretNumber * 2048U)
    return secretNumber % base
}

fun getNthSecretNumber(secretNumber: UInt, n: Int) = (0 until n)
    .fold(secretNumber) { accumulator, _ -> nextSecretNumber(accumulator) }

fun part1() {
    Path("input.txt")
        .readLines()
        .map { it.toUInt() }
        .map { it to getNthSecretNumber(it, 2000) }
        .onEach { (number, result) -> println("$number: $result") }
        .sumOf { (_, result) -> result.toULong() }
        .also(::println)
}

/*class Cache {
    private val valueCache: Map<Int, Int>
    val indexCache: List<Int>
    private val offsetCache: Map<Int, Int>
    val existingSequences: Set<List<Int>>

    init {
        val byValue = mutableMapOf<Int, Int>()
        val byIndex = mutableListOf(1)
        val offsets = mutableMapOf(1 to 0)

        var lastValue = 1
        for (i in 0..<PERIOD) {
            val next = nextSecretNumber(lastValue)
            byValue[lastValue] = next
            byIndex.add(next)
            offsets[next] = i + 1
            lastValue = next
        }
        valueCache = byValue
        indexCache = byIndex
        offsetCache = offsets

        existingSequences = findExistingSequences()
    }

    private fun findExistingSequences(): Set<List<Int>> {
        return (0..<PERIOD)
            .map { startIndex ->
                (startIndex..<startIndex + 5)
                    .map { index -> nthPrice(1, index) }
                    .windowed(2)
                    .map { (priceA, priceB) ->
                        priceB - priceA
                    }
            }
            .toSet()
    }

    fun next(secretNumber: Int) = valueCache[secretNumber]!!

    fun getNth(secretNumber: Int, n: Int) = offsetOf(secretNumber)
        .let { indexCache[(it + n) % PERIOD] }

    fun offsetOf(secretNumber: Int) = offsetCache[secretNumber]!!

    fun nthPrice(secretNumber: Int, n: Int) = getNth(secretNumber, n)
        .let { it % 10 }

    private fun priceBySequence(
        priceIndex: Int,
        sequence: List<Int>,
    ): Int {
        @Suppress("NAME_SHADOWING")
        var priceIndex = priceIndex

        val deltasWindow = MutableList(4) { index ->
            val nextIndex = (index + 1) % PERIOD
            (indexCache[nextIndex] % 10) - (indexCache[index] % 10)
        }
        priceIndex = (priceIndex + 5) % PERIOD
        var latestPrice = (indexCache[priceIndex] % 10)
        outer@ while (true) {
            for (i in 0 until 4) {
                if (sequence[i] != deltasWindow[i]) {
                    priceIndex = (priceIndex + 1) % PERIOD
                    val newPrice = (indexCache[priceIndex] % 10)
                    for (j in 0 until 3) {
                        deltasWindow[j] = deltasWindow[j + 1]
                    }
                    deltasWindow[3] = newPrice - latestPrice
                    latestPrice = newPrice
                    continue@outer
                }
            }
            return latestPrice
        }
        /*return generateSequence(0) { it + 1 }
            .map { index ->
                index to (index..<index + 5)
                    .asSequence()
                    .map { nthPrice(secretNumber, it) }
                    .windowed(2)
                    .map { (priceA, priceB) ->
                        priceB - priceA
                    }
            }
            .first { (index, deltaSequence) ->
                deltaSequence.zip(sequence).all { (a, b) -> a == b }
            }
            .let { (index, deltaSequence) -> index + 4 }
            .let { index -> nthPrice(secretNumber, index) }*/
    }

    fun calculatePriceMap(secretNumber: Int): Map<List<Int>, Int> {
        val priceIndex = offsetCache[secretNumber]!!
        return existingSequences
            .take(1000)
            .associateWith { sequence ->
                val price = priceBySequence(priceIndex, sequence)
//                println("price for sequence $sequence: $price")
//                System.out.flush()
                price
            }
    }
}*/

data class DeltaSequence(var deltas: List<Int>)

fun main() {
    // part1()

    // period 33554430

    /*val cache = Cache()
    println(cache.existingSequences.toSet().size)*/

//    println(cache.getNth(123U, 1))
    /*println("starting calculation...")
    System.out.flush()
    measureTimedValue { cache.calculatePriceMap(123) }.duration.also(::println)*/

    println("parsing input and generating sequence...")
    System.out.flush()

    val monkeyMaps = Path("input.txt")
        .readLines()
        .map { it.toUInt() }
        .associateWith { _ -> mutableMapOf<DeltaSequence, Int>() }

    val sequence = generateSequence(1U) { nextSecretNumber(it) }
        .take(PERIOD.toInt())
        .toList()

    val existingMonkeys = monkeyMaps
        .keys
        .toSet()

    println("building up data structure...")
    System.out.flush()

    val activeMonkeys = mutableMapOf<UInt, Int>()
    for (i in 0U until 2U * PERIOD) {
        if (i % 10000U == 0U) {
            val relative = (i + 1U).toDouble() / (2U * PERIOD).toDouble() * 100.0
            println("${i + 1U} of ${2U * PERIOD} ($relative %)")
            System.out.flush()
        }
        val number = sequence[(i % PERIOD).toInt()]
        val isMonkey = number in existingMonkeys
        if (isMonkey && number !in activeMonkeys) {
            activeMonkeys[number] = i.toInt()
        }

        val prices = (i until i + 5U)
            .map { (sequence[(it % PERIOD).toInt()] % 10U).toInt() }
        val deltaSequence = prices
            .windowed(2)
            .map { (a, b) -> b - a }
            .let { DeltaSequence(it) }

        for ((activeMonkey, start) in activeMonkeys.filter { (monkey, start) -> i.toInt() <= start + 2005 }) {
            monkeyMaps[activeMonkey]!!.putIfAbsent(deltaSequence, prices.last())
        }
    }

    println("finding best price...")
    System.out.flush()

    val allSequences = monkeyMaps
        .values
        .fold(setOf<DeltaSequence>()) { set, monkeyMap ->
            set.union(monkeyMap.keys)
        }

    allSequences
        .map { deltaSequence ->
            deltaSequence to monkeyMaps
                .values
                .sumOf { it[deltaSequence] ?: 0 }
        }
        .maxBy { it.second }
        .also(::println)

    /*for (deltaSequence in monkeyMaps[1U]!!.keys) {
        monkeyMaps
            .values
            .sumOf { it[deltaSequence]!! }
    }*/

    /*val existingDeltaSequences = (0..<PERIOD)
        .map { startIndex -> (startIndex..<startIndex + 5)
            .map { (getNthCached(secretNumber, it) % 10U).toInt() }
            .windowed(2)
            .map { (a, b) -> b - a }
        }
        .filter { it.sum() > 0 }
        .toSet()

    println(existingDeltaSequences.size)*/


    /*val secretNumber = 2024U
    deltaSequences
        .mapIndexed { i, deltaSequence ->
            i to (0..<PERIOD + 5)
                .asSequence()
                .windowed(5)
                .map { indices ->
                    indices.first() to indices
                        .map { (getNthCached(secretNumber, it) % 10U).toInt() }
                        .windowed(2)
                        .map { (priceA, priceB) -> priceB - priceA }
                }
                .first { (i, deltas) -> deltas == deltaSequence }
                .also { (i, deltas) -> println(i) }
        }
        .forEach { (deltaSequenceIndex, priceIndex) -> println("$deltaSequenceIndex: $priceIndex") }*/
}
