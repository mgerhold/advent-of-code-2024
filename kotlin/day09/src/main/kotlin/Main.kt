package org.example

import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.time.measureTime

sealed class DiskBlock(val length: Int) {
    class Empty(length: Int) : DiskBlock(length) {
        override fun toString() = ".".repeat(length)
    }

    class File(length: Int, val id: Int) : DiskBlock(length) {
        override fun toString() = id.toString().repeat(length)
    }
}

fun part1(blocks: MutableList<DiskBlock>): ULong {
    var gapIndex = blocks
        .withIndex()
        .dropWhile { (_, block) -> block is DiskBlock.File }
        .map { (i, _) -> i }
        .first()

    outer@ while (blocks.last() is DiskBlock.File) {
        var file = blocks.removeLast() as DiskBlock.File
        while (file.length > 0) {
            if (blocks[gapIndex].length == file.length) {
                blocks.removeAt(gapIndex)
                blocks.add(gapIndex, DiskBlock.File(file.length, file.id))
                file = DiskBlock.File(0, file.id)
                while (gapIndex < blocks.size && blocks[gapIndex] is DiskBlock.File) {
                    ++gapIndex
                }
                if (gapIndex == blocks.size) {
                    break@outer
                }
            } else if (file.length > blocks[gapIndex].length) {
                if (gapIndex == blocks.indices.last()) {
                    blocks.removeLast()
                    blocks.add(DiskBlock.File(file.length, file.id))
                    break@outer
                }
                val gapLength = blocks[gapIndex].length
                blocks.removeAt(gapIndex)
                blocks.add(gapIndex, DiskBlock.File(gapLength, file.id))
                file = DiskBlock.File(file.length - gapLength, file.id)
                while (gapIndex < blocks.size && blocks[gapIndex] is DiskBlock.File) {
                    ++gapIndex
                }
                if (gapIndex == blocks.size) {
                    break@outer
                }
            } else {
                check(file.length < blocks[gapIndex].length)
                val gapLength = blocks[gapIndex].length
                blocks.removeAt(gapIndex)
                blocks.add(gapIndex, DiskBlock.File(file.length, file.id))
                ++gapIndex
                blocks.add(gapIndex, DiskBlock.Empty(gapLength - file.length))
                file = DiskBlock.File(0, file.id)
            }
        }
        while (blocks.last() is DiskBlock.Empty) {
            blocks.removeLast()
        }
    }

    return blocks
        .fold(0UL to 0) { (checksum, index), block ->
            val file = block as DiskBlock.File
            val sum = (index..<index + file.length)
                .sumOf { it.toULong() * file.id.toULong() }
            checksum + sum to index + file.length
        }
        .first
}

fun part2(blocks: MutableList<DiskBlock>): ULong {
    var nextFileId = (blocks.last() as DiskBlock.File).id
    while (nextFileId > 0) {
        val fileIndex = blocks
            .withIndex()
            .last { (_, block) -> block is DiskBlock.File && block.id == nextFileId }
            .index
        val gapIndex = blocks
            .take(fileIndex)
            .withIndex()
            .firstOrNull { (_, block) -> block is DiskBlock.Empty && block.length >= blocks[fileIndex].length }
            ?.index
        if (gapIndex == null) {
            --nextFileId
            continue
        }
        val gapLength = blocks[gapIndex].length
        val file = blocks.removeAt(fileIndex)
        blocks.add(fileIndex, DiskBlock.Empty(file.length))

        blocks.removeAt(gapIndex)
        val fileLength = file.length
        blocks.add(gapIndex, file)
        if (gapLength > fileLength) {
            blocks.add(gapIndex + 1, DiskBlock.Empty(gapLength - fileLength))
        }
        --nextFileId
    }

    return blocks
        .fold(0UL to 0) { (checksum, index), block ->
            when (block) {
                is DiskBlock.Empty -> checksum to index + block.length
                is DiskBlock.File -> checksum + (index..<index + block.length)
                    .sumOf { it.toULong() * block.id.toULong() } to index + block.length
            }
        }
        .first
}

fun main() {
    val blocks = Path("input.txt")
        .readText()
        .trim()
        .chunked(2)
        .fold(0 to mutableListOf<DiskBlock>()) { (nextId, blocks), symbols ->
            val file = DiskBlock.File(symbols.first().digitToInt(), nextId)
            val emptiness =
                if (symbols.length == 1 || symbols[1] == '0') null else DiskBlock.Empty(symbols[1].digitToInt())
            blocks.add(file)
            if (emptiness != null) {
                blocks.add(emptiness)
            }
            nextId + 1 to blocks
        }
        .second

    println(part1(blocks.toMutableList()))
    println(
        measureTime {
            println(part2(blocks.toMutableList()))
        }
    )
}
