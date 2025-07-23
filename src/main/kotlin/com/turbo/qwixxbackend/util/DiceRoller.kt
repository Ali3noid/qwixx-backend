package com.turbo.qwixxbackend.util

import com.turbo.qwixxbackend.util.DiceColor.*
import kotlin.random.Random

/**
 * Utility class for rolling dice
 */
object DiceRoller {

    private val random = Random.Default

    /**
     * Rolls all 6 dice
     * @return Map of dice colors to their rolled values (1-6)
     */
    fun rollAllDice(): Map<DiceColor, Int> {
        return mapOf(
            WHITE_1 to rollDie(),
            WHITE_2 to rollDie(),
            RED to rollDie(),
            YELLOW to rollDie(),
            GREEN to rollDie(),
            BLUE to rollDie()
        )
    }

    /**
     * Rolls a 6-sided die
     * @return Random value between 1 and 6
     */
    private fun rollDie(): Int {
        return random.nextInt(1, 7)
    }

    /**
     * Calculates all possible moves for the active player.
     * Active player can use: white1 + white2, white1 + any color, white2 + any color
     */
    fun calculateActivePlayerMoves(diceValues: Map<DiceColor, Int>): List<DiceCombination> {
        val white1 = diceValues[WHITE_1]!!
        val white2 = diceValues[WHITE_2]!!
        val red = diceValues[RED]!!
        val yellow = diceValues[YELLOW]!!
        val green = diceValues[GREEN]!!
        val blue = diceValues[BLUE]!!

        return listOf(
            // White dice combination
            DiceCombination(
                value = white1 + white2,
                diceUsed = listOf(WHITE_1, WHITE_2),
                availableForRows = listOf(RowColor.RED, RowColor.YELLOW, RowColor.GREEN, RowColor.BLUE)
            ),
            // White1 + colored dice combinations
            DiceCombination(
                value = white1 + red,
                diceUsed = listOf(WHITE_1, RED),
                availableForRows = listOf(RowColor.RED)
            ),
            DiceCombination(
                value = white1 + yellow,
                diceUsed = listOf(WHITE_1, YELLOW),
                availableForRows = listOf(RowColor.YELLOW)
            ),
            DiceCombination(
                value = white1 + green,
                diceUsed = listOf(WHITE_1, GREEN),
                availableForRows = listOf(RowColor.GREEN)
            ),
            DiceCombination(
                value = white1 + blue,
                diceUsed = listOf(WHITE_1, BLUE),
                availableForRows = listOf(RowColor.BLUE)
            ),
            // White2 + colored dice combinations
            DiceCombination(
                value = white2 + red,
                diceUsed = listOf(WHITE_2, RED),
                availableForRows = listOf(RowColor.RED)
            ),
            DiceCombination(
                value = white2 + yellow,
                diceUsed = listOf(WHITE_2, YELLOW),
                availableForRows = listOf(RowColor.YELLOW)
            ),
            DiceCombination(
                value = white2 + green,
                diceUsed = listOf(WHITE_2, GREEN),
                availableForRows = listOf(RowColor.GREEN)
            ),
            DiceCombination(
                value = white2 + blue,
                diceUsed = listOf(WHITE_2, BLUE),
                availableForRows = listOf(RowColor.BLUE)
            )
        ).distinctBy { "${it.value}-${it.availableForRows.joinToString()}" } // Remove duplicates
    }

    /**
     * Calculates possible moves for non-active players.
     * Non-active players can only use the sum of two white dice
     */
    fun calculateNonActivePlayerMoves(diceValues: Map<DiceColor, Int>): List<DiceCombination> {
        val white1 = diceValues[WHITE_1]!!
        val white2 = diceValues[WHITE_2]!!

        return listOf(
            DiceCombination(
                value = white1 + white2,
                diceUsed = listOf(WHITE_1, WHITE_2),
                availableForRows = listOf(RowColor.RED, RowColor.YELLOW, RowColor.GREEN, RowColor.BLUE)
            )
        )
    }

    /**
     * Formats dice roll result for logging/debugging
     */
    fun printableDiceRoll(diceValues: Map<DiceColor, Int>): String {
        return buildString {
            append("Dice Roll: ")
            append("W1:${diceValues[WHITE_1]} ")
            append("W2:${diceValues[WHITE_2]} ")
            append("R:${diceValues[RED]} ")
            append("Y:${diceValues[YELLOW]} ")
            append("G:${diceValues[GREEN]} ")
            append("B:${diceValues[BLUE]}")
        }
    }

    /**
     * Validates that dice values are within the valid range (1-6)
     */
    fun validateDiceValues(diceValues: Map<DiceColor, Int>): Boolean {
        return diceValues.all { (_, value) -> value in 1..6 } &&
                diceValues.keys.containsAll(DiceColor.entries)
    }
}

/**
 * Represents a combination of dice and their possible usage
 */
data class DiceCombination(
    val value: Int,
    val diceUsed: List<DiceColor>,
    val availableForRows: List<RowColor>,
    val isPass: Boolean = false
) {
    companion object {
        fun passMove(): DiceCombination = DiceCombination(
            value = 0,
            diceUsed = emptyList(),
            availableForRows = emptyList(),
            isPass = true
        )
    }
}

/**
 * Enum representing dice colors
 */
enum class DiceColor {
    WHITE_1,
    WHITE_2,
    RED,
    YELLOW,
    GREEN,
    BLUE
}

/**
 * Enum representing row colors on the scoresheet
 */
enum class RowColor {
    RED,
    YELLOW,
    GREEN,
    BLUE
}

