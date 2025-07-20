package com.turbo.qwixxbackend.entity

import com.turbo.qwixxbackend.util.DiceColor
import com.turbo.qwixxbackend.util.DiceCombination
import com.turbo.qwixxbackend.util.DiceRoller
import com.turbo.qwixxbackend.util.RowColor
import com.turbo.qwixxbackend.util.RowColor.*

/**
 * Immutable scoresheet
 */
data class ScoreSheet(
    val redRow: QwixxRow = QwixxRow.ascendingRow(),
    val yellowRow: QwixxRow = QwixxRow.ascendingRow(),
    val greenRow: QwixxRow = QwixxRow.descendingRow(),
    val blueRow: QwixxRow = QwixxRow.descendingRow(),
    val penalties: Int = 0
) {

    /**
     * Marks a number in a specified row color
     * @param color Row color to mark
     * @param number Number to mark
     * @return New ScoreSheet with marked number or null if move is invalid
     */
    fun markNumber(color: RowColor, number: Int): ScoreSheet? {
        if (!isValidMove(color, number)) return null

        return when (color) {
            RED -> copy(redRow = redRow.mark(number))
            YELLOW -> copy(yellowRow = yellowRow.mark(number))
            GREEN -> copy(greenRow = greenRow.mark(number))
            BLUE -> copy(blueRow = blueRow.mark(number))
        }
    }

    /**
     * Checks if a move is valid in the current state
     * @param color Row color
     * @param number Number to check
     * @return true if the move is valid
     */
    fun isValidMove(color: RowColor, number: Int): Boolean {
        val row = getRow(color)
        return row.canMark(number) && !row.isLocked
    }

    /**
     * Gets all valid moves based on dice roll and current sheet state
     * @param diceRoll Current dice values
     * @param isActivePlayer Whether this is for active player (more combinations)
     * @return List of possible moves that can actually be made
     */
    fun getValidMoves(
        diceRoll: Map<DiceColor, Int>,
        isActivePlayer: Boolean = true
    ): List<DiceCombination> {
        val allCombinations = if (isActivePlayer) {
            DiceRoller.calculateActivePlayerMoves(diceRoll)
        } else {
            DiceRoller.calculateNonActivePlayerMoves(diceRoll)
        }

        // Filter combinations to only include moves that are actually valid in current state
        return allCombinations.map { combination ->
            val validRows = combination.availableForRows.filter { rowColor ->
                isValidMove(rowColor, combination.value)
            }
            combination.copy(availableForRows = validRows)
        }.filter { it.availableForRows.isNotEmpty() }
    }

    /**
     * Adds a penalty to the score sheet
     * @return New ScoreSheet with incremented penalty
     */
    fun addPenalty(): ScoreSheet {
        return copy(penalties = penalties + 1)
    }

    /**
     * Locks a row (when last number is marked)
     * @param color Row color to lock
     * @return New ScoreSheet with locked row
     */
    fun lockRow(color: RowColor): ScoreSheet {
        return when (color) {
            RED -> copy(redRow = redRow.lock())
            YELLOW -> copy(yellowRow = yellowRow.lock())
            GREEN -> copy(greenRow = greenRow.lock())
            BLUE -> copy(blueRow = blueRow.lock())
        }
    }

    /**
     * Checks if a row can be locked (minimum 5 marked + last number marked)
     */
    fun canLockRow(color: RowColor): Boolean {
        val row = getRow(color)
        return row.canBeLocked()
    }

    /**
     * Gets count of marked numbers in a specified row
     */
    fun getMarkedCount(color: RowColor): Int {
        return getRow(color).markedCount
    }

    /**
     * Checks if the row is locked
     */
    fun isRowLocked(color: RowColor): Boolean {
        return getRow(color).isLocked
    }

    /**
     * Calculates total score
     * @return Total points (positive row scores minus penalty points)
     */
    fun calculateScore(): Int {
        return redRow.calculateScore() +
                yellowRow.calculateScore() +
                greenRow.calculateScore() +
                blueRow.calculateScore() -
                (penalties * 5)
    }

    /**
     * Gets score breakdown for a detailed view
     */
    fun getScoreBreakdown(): ScoreBreakdown {
        return ScoreBreakdown(
            redScore = redRow.calculateScore(),
            yellowScore = yellowRow.calculateScore(),
            greenScore = greenRow.calculateScore(),
            blueScore = blueRow.calculateScore(),
            penaltyScore = penalties * 5,
            totalScore = calculateScore()
        )
    }

    /**
     * Checks if the game should end (4 penalties or 2+ locked rows)
     */
    fun shouldEndGame(): Boolean {
        val lockedRowsCount = listOf(redRow, yellowRow, greenRow, blueRow).count { it.isLocked }
        return penalties >= 4 || lockedRowsCount >= 2
    }

    private fun getRow(color: RowColor): QwixxRow = when (color) {
        RED -> redRow
        YELLOW -> yellowRow
        GREEN -> greenRow
        BLUE -> blueRow
    }
}

/**
 * Represents a single row in a Qwixx scoresheet
 */
data class QwixxRow(
    val values: List<Int>,
    val markedPositions: Set<Int> = emptySet(),
    val isLocked: Boolean = false
) {

    companion object {
        fun ascendingRow(): QwixxRow = QwixxRow((2..12).toList())
        fun descendingRow(): QwixxRow = QwixxRow((12 downTo 2).toList())
    }

    val markedCount: Int get() = markedPositions.size

    /**
     * Checks if a value can be marked
     * Rules: Can only mark values to the right of the rightmost marked value
     */
    fun canMark(value: Int): Boolean {
        if (isLocked || value !in values) return false

        val lastMarkedIndex = markedPositions.maxOfOrNull { values.indexOf(it) } ?: -1
        val targetIndex = values.indexOf(value)

        return targetIndex > lastMarkedIndex
    }

    /**
     * Marks a value in the row
     * @param value Value to mark
     * @return New QwixxRow with marked value
     */
    fun mark(value: Int): QwixxRow {
        return if (canMark(value)) {
            val newMarkedPositions = markedPositions + value
            val shouldAutoLock = value == values.last() && markedCount >= 4

            copy(
                markedPositions = newMarkedPositions,
                isLocked = shouldAutoLock
            )
        } else {
            this
        }
    }

    /**
     * Manually locks the row
     */
    fun lock(): QwixxRow = copy(isLocked = true)

    /**
     * Checks if the row can be locked (minimum 5 marks + last value marked)
     */
    fun canBeLocked(): Boolean {
        return markedCount >= 5 && values.last() in markedPositions
    }

    /**
     * Calculates score for this row
     * Formula: n * (n + 1) / 2 where n is the number of marked positions
     */
    fun calculateScore(): Int {
        val n = markedCount
        return n * (n + 1) / 2
    }

    /**
     * Gets the rightmost marked value index
     */
    fun getRightmostMarkedIndex(): Int {
        return markedPositions.maxOfOrNull { values.indexOf(it) } ?: -1
    }

    /**
     * Gets values that can still be marked
     */
    fun getAvailableValues(): List<Int> {
        val rightmostIndex = getRightmostMarkedIndex()
        return values.drop(rightmostIndex + 1)
    }
}

/**
 * Detailed score breakdown
 */
data class ScoreBreakdown(
    val redScore: Int,
    val yellowScore: Int,
    val greenScore: Int,
    val blueScore: Int,
    val penaltyScore: Int,
    val totalScore: Int
)