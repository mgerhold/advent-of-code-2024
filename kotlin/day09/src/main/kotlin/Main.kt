package org.example

import kotlin.io.path.Path
import kotlin.io.path.readText

sealed class DiskBlock(val length: Int) {
    class Empty(length: Int) : DiskBlock(length) {
        override fun toString() = "Empty($length)" //".".repeat(length)
    }

    class File(length: Int, val id: Int) : DiskBlock(length) {
        override fun toString() = "File(id=$id, length=$length)" //id.toString().repeat(length)
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
    @Suppress("NAME_SHADOWING")
    var blocks = blocks

    val lastFileId = (blocks.last() as DiskBlock.File).id

    for (fileId in (0..lastFileId).reversed()) {
        val fileIndex = blocks
            .withIndex()
            .last { (_, block) -> block is DiskBlock.File && block.id == fileId }
            .index

        val fileLength = blocks[fileIndex].length

        val gapIndex = blocks
            .take(fileIndex)
            .withIndex()
            .firstOrNull { (_, block) -> block is DiskBlock.Empty && block.length >= fileLength }
            ?.index

        if (gapIndex == null) {
            continue
        }

        val file = blocks[fileIndex] as DiskBlock.File
        blocks[fileIndex] = DiskBlock.Empty(fileLength)
        val gapLength = blocks[gapIndex].length
        blocks[gapIndex] = file
        if (fileLength < gapLength) {
            blocks.add(gapIndex + 1, DiskBlock.Empty(gapLength - fileLength))
        }

        // Perform cleanup.
        val newList = mutableListOf<DiskBlock>()
        var gapAccumulator = 0
        for (block in blocks) {
            if (block is DiskBlock.Empty) {
                gapAccumulator += block.length
            } else {
                if (gapAccumulator > 0) {
                    newList.add(DiskBlock.Empty(gapAccumulator))
                    gapAccumulator = 0
                }
                newList.add(block)
            }
        }

        blocks = newList
    }

    return blocks
        .fold(0UL to 0) { (checksum, index), block ->
            when (block) {
                is DiskBlock.Empty -> checksum to index + block.length
                is DiskBlock.File -> {
                    val sum = (index..<index + block.length)
                        .sumOf { it.toULong() * block.id.toULong() }
                    checksum + sum to index + block.length
                }
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
    println(part2(blocks.toMutableList()))
}
