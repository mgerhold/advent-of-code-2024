package org.example

import javafx.animation.KeyFrame
import javafx.animation.Timeline
import kotlin.io.path.Path
import kotlin.io.path.readLines
import kotlin.math.abs

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.GridPane
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.stage.Stage
import javafx.util.Duration
import kotlin.math.sqrt
import kotlin.time.measureTimedValue

class MatrixRenderer : Application() {

    private val cellSize = 5.0 // Size of each cell
    private lateinit var matrix: Array<Array<Int>>
    private lateinit var rectangles: Array<Array<Rectangle>>
    private val gridPane = GridPane()
    private lateinit var timeline: Timeline

    companion object {
        private lateinit var matrixProvider: () -> Array<Array<Int>>
        private lateinit var speedProvider: () -> Double?

        fun setMatrixProvider(provider: () -> Array<Array<Int>>) {
            matrixProvider = provider
        }

        fun setSpeedProvider(provider: () -> Double?) {
            speedProvider = provider
        }
    }

    override fun start(primaryStage: Stage) {
        // Initialize the matrix and rectangles
        matrix = matrixProvider()
        rectangles = Array(matrix.size) { i ->
            Array(matrix[i].size) { j ->
                Rectangle(cellSize, cellSize).apply {
                    fill = Color.WHITE // Initial fill color
                    stroke = null
                    gridPane.add(this, j, i)
                }
            }
        }

        val scene = Scene(gridPane, cellSize * matrix[0].size, cellSize * matrix.size)
        primaryStage.title = "Matrix Renderer"
        primaryStage.scene = scene
        primaryStage.show()

        startMatrixAnimation()
    }

    private fun startMatrixAnimation() {
        timeline = Timeline().apply {
            cycleCount = Timeline.INDEFINITE // Run indefinitely
        }
        updateAnimationSpeed() // Set initial speed
    }

    private fun updateAnimationSpeed() {
        val providedSpeed = speedProvider()
        if (providedSpeed == null) {
            timeline.stop()
        } else {
            val duration = Duration.millis(providedSpeed) // Fetch the current speed
            timeline.stop() // Stop the existing timeline
            timeline.keyFrames.clear()
            if (duration != null) {
                timeline.keyFrames.add(
                    KeyFrame((duration), {
                        matrix = matrixProvider() // Fetch the updated matrix from the callback
                        renderMatrix()
                        updateAnimationSpeed()
                    })
                )
                timeline.play() // Restart the timeline with the new speed
            }
        }
    }

    private fun renderMatrix() {
        // Update rectangle colors based on the updated matrix
        for (i in matrix.indices) {
            for (j in matrix[i].indices) {
                rectangles[i][j].fill = if (matrix[i][j] == 1) Color.LIGHTGREEN else Color.BLACK
            }
        }
    }
}

data class Vec2(val x: Int, val y: Int) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun div(dividend: Int) = Vec2(x / dividend, y / dividend)

    companion object {
        fun fromString(string: String) = string
            .substringAfter("=")
            .split(",")
            .map(String::toInt)
            .let { (x, y) -> Vec2(x, y) }
    }
}

fun calculateStandardDeviation(values: Set<Double>): Double {
    val mean = values.sum() / values.size.toDouble()
    return values
        .map { abs(mean - it) }
        .sumOf { it * it }
        .let(::sqrt)
}

val mapSize = Vec2(101, 103)

data class Robot(val position: Vec2, val velocity: Vec2) {
    fun move(mapSize: Vec2): Robot {
        require(abs(velocity.x) <= mapSize.x && abs(velocity.y) <= mapSize.y)

        var newPosition = position + velocity

        if (newPosition.x < 0) {
            newPosition = Vec2(newPosition.x + mapSize.x, newPosition.y)
        } else if (newPosition.x >= mapSize.x) {
            newPosition = Vec2(newPosition.x - mapSize.x, newPosition.y)
        }

        if (newPosition.y < 0) {
            newPosition = Vec2(newPosition.x, newPosition.y + mapSize.y)
        } else if (newPosition.y >= mapSize.y) {
            newPosition = Vec2(newPosition.x, newPosition.y - mapSize.y)
        }

        return Robot(newPosition, velocity)
    }

    companion object {
        fun fromString(string: String) = string
            .split(" ")
            .map(Vec2::fromString)
            .let { (position, velocity) -> Robot(position, velocity) }
    }
}

fun drawMap(mapSize: Vec2, robots: List<Robot>) {
    val positionCounts = robots
        .map { it.position }
        .groupingBy { it }
        .eachCount()

    for (row in 0..<mapSize.y) {
        for (column in 0..<mapSize.x) {
            val count = positionCounts[Vec2(column, row)]
            print(
                when (count) {
                    null -> '.'
                    else -> count
                }
            )
        }
        println()
    }
}

fun calculateSafetyFactor(mapSize: Vec2, robots: List<Robot>): Int {
    val positionCounts = robots
        .map { it.position }
        .groupingBy { it }
        .eachCount()

    val quadrantSize = mapSize / 2
    return arrayOf(
        Vec2(0, 0),
        Vec2(quadrantSize.x + 1, 0),
        Vec2(0, quadrantSize.y + 1),
        Vec2(quadrantSize.x + 1, quadrantSize.y + 1)
    )
        .map {
            positionCounts
                .filter { (position, _) ->
                    position.x in (it.x..<it.x + quadrantSize.x) && position.y in (it.y..<it.y + quadrantSize.y)
                }
                .values
                .sum()
        }
        .onEach(::println)
        .fold(1) { accumulator, count -> accumulator * count }
}

fun part1(robots: List<Robot>) {
    val numSeconds = 100

    val predictedRobots = robots
        .map { (0..<numSeconds).fold(it) { robot, _ -> robot.move(mapSize) } }

    drawMap(mapSize, predictedRobots)

    val result = calculateSafetyFactor(mapSize, predictedRobots)
    println("Safety factor: $result")
}

fun createMatrix(mapSize: Vec2, robots: List<Robot>): Array<Array<Int>> {
    val positions = robots
        .map { it.position }
        .toSet()
    return Array(mapSize.y) { rowIndex ->
        Array(mapSize.x) { columnIndex -> if (Vec2(columnIndex, rowIndex) in positions) 1 else 0 }
    }
}

fun findMinDeviation(values: List<Set<Double>>) = values.minOf(::calculateStandardDeviation)

fun findMinDeviations(robots: List<Robot>): Pair<Double, Double> {
    @Suppress("NAME_SHADOWING")
    var robots = robots
    val positionsOverTime = (0..<150)
        .map {
            robots = robots.map { it.move(mapSize) }
            robots
                .map { it.position }
                .toSet()
        }

    val xPositionsOverTime = positionsOverTime
        .map { it.map { position -> position.x.toDouble() }.toSet() }
    val yPositionsOverTime = positionsOverTime
        .map { it.map { position -> position.y.toDouble() }.toSet() }

    return findMinDeviation(xPositionsOverTime) to findMinDeviation(yPositionsOverTime)
}

data class Oscillation(val phase: ULong, val period: ULong)

fun findOscillations(robots: List<Robot>): Pair<Oscillation, Oscillation> {
    var xPhase: ULong? = null
    var yPhase: ULong? = null
    var xPeriod: ULong? = null
    var yPeriod: ULong? = null

    val (xMinDeviation, yMinDeviation) = findMinDeviations(robots)

    @Suppress("NAME_SHADOWING")
    var robots = robots

    var frame = 0UL

    while (xPhase == null || yPhase == null || xPeriod == null || yPeriod == null) {
        robots = robots.map { it.move(mapSize) }
        ++frame

        val xDeviation = calculateStandardDeviation(robots.map { it.position.x.toDouble() }.toSet())

        if (xDeviation == xMinDeviation) {
            if (xPhase == null) {
                xPhase = frame
            } else if (xPeriod == null) {
                xPeriod = frame - xPhase
            }
        }

        val yDeviation = calculateStandardDeviation(robots.map { it.position.y.toDouble() }.toSet())

        if (yDeviation == yMinDeviation) {
            if (yPhase == null) {
                yPhase = frame
            } else if (yPeriod == null) {
                yPeriod = frame - yPhase
            }
        }
    }

    return Oscillation(xPhase, xPeriod) to Oscillation(yPhase, yPeriod)
}

fun part2(robots: List<Robot>): ULong {
    val (xOscillation, yOscillation) = findOscillations(robots)

    var x = xOscillation.phase
    var y = yOscillation.phase

    while (x != y) {
        if (x < y) {
            x += xOscillation.period
        } else {
            y += yOscillation.period
        }
    }

    return x
}

fun parseRobots() = Path("input.txt")
    .readLines()
    .filter { !it.startsWith("//") }
    .map(Robot::fromString)

fun main() {
    part1(parseRobots())

    var (robots, parseDuration) = measureTimedValue { parseRobots() }
    val (part2Solution, part2duration) = measureTimedValue { part2(robots) }
    println("parse duration: $parseDuration")
    println("part 2 duration: $part2duration")
    println(part2Solution)

    println(findMinDeviations(robots))
    val (deviations, deviationsDuration) = measureTimedValue { findMinDeviations(robots) }
    val (xMinDeviation, yMinDeviation) = deviations

    var matrixProviderCount = 0
    var speedProviderCount = 0
    var frame = 0UL
    var lastFrame = 0UL

    val matrixProvider = {
        ++matrixProviderCount
        robots = robots
            .map { it.move(mapSize) }
        ++frame
        createMatrix(mapSize, robots)
    }
    val speedProvider = {
        ++speedProviderCount
        val xDeviation = calculateStandardDeviation(robots.map { it.position.x.toDouble() }.toSet())
        val yDeviation = calculateStandardDeviation(robots.map { it.position.y.toDouble() }.toSet())
        if (frame % 1000UL == 0UL) {
            println(frame)
        }
        if (xDeviation == xMinDeviation && yDeviation == yMinDeviation) {
            val delta = frame - lastFrame
            lastFrame = frame
            println("frame: $frame, delta: $delta")
            null
        } else {
            0.1
        }
    }

    MatrixRenderer.setMatrixProvider(matrixProvider)
    MatrixRenderer.setSpeedProvider(speedProvider)
    Application.launch(MatrixRenderer::class.java)
}
