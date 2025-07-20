package com.turbo.qwixxbackend.entity

import com.turbo.qwixxbackend.util.DiceColor
import com.turbo.qwixxbackend.util.RowColor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("ScoreSheet Tests")
class ScoreSheetTest {

    @Nested
    @DisplayName("QwixxRow Tests")
    inner class QwixxRowTests {

        @Test
        fun `should create ascending row with values from 2 to 12`() {
            // when
            val row = QwixxRow.ascendingRow()

            // then
            with(row) {
                assertThat(values).isEqualTo((2..12).toList())
                assertThat(markedPositions).isEmpty()
                assertThat(isLocked).isFalse()
            }
        }

        @Test
        fun `should create descending row with values from 12 to 2`() {
            // when
            val row = QwixxRow.descendingRow()

            // then
            with(row) {
                assertThat(values).isEqualTo((12 downTo 2).toList())
                assertThat(markedPositions).isEmpty()
                assertThat(isLocked).isFalse()
            }
        }

        @Test
        fun `canMark should return true for valid values`() {
            // given
            val row = QwixxRow.ascendingRow()

            // then
            with(row) {
                assertThat(canMark(2)).isTrue()
                assertThat(canMark(12)).isTrue()
            }
        }

        @Test
        fun `canMark should return false for values not in the row`() {
            // given
            val row = QwixxRow.ascendingRow()

            // then
            with(row) {
                assertThat(canMark(1)).isFalse()
                assertThat(canMark(13)).isFalse()
            }
        }

        @Test
        fun `canMark should return false for locked row`() {
            // given
            val row = QwixxRow.ascendingRow().lock()

            // then
            assertThat(row.canMark(2)).isFalse()
        }

        @Test
        fun `canMark should only allow marking values to the right of rightmost marked value`() {
            // given
            val row = QwixxRow.ascendingRow().mark(5)

            // then
            with(row) {
                assertThat(canMark(2)).isFalse()
                assertThat(canMark(5)).isFalse()
                assertThat(canMark(6)).isTrue()
                assertThat(canMark(12)).isTrue()
            }
        }

        @Test
        fun `mark should add value to marked positions`() {
            // given
            val row = QwixxRow.ascendingRow()

            // when
            val markedRow = row.mark(5)

            // then
            assertThat(markedRow.markedPositions).containsExactly(5)
        }

        @Test
        fun `mark should not change row if value cannot be marked`() {
            // given
            val row = QwixxRow.ascendingRow().mark(5)

            // when
            val unchangedRow = row.mark(4)

            // then
            assertThat(unchangedRow).isEqualTo(row)
        }

        @Test
        fun `mark should auto-lock row when marking last value with at least 5 marks`() {
            // given
            val row = QwixxRow.ascendingRow()
                .mark(2).mark(3).mark(4).mark(5).mark(6)

            // when
            val lockedRow = row.mark(12)

            // then
            assertThat(lockedRow.isLocked).isTrue()
        }

        @Test
        fun `mark should not auto-lock row when marking last value with less than 5 marks`() {
            // given
            val row = QwixxRow.ascendingRow()
                .mark(2).mark(3).mark(4)

            // when
            val notLockedRow = row.mark(12)

            // then
            assertThat(notLockedRow.isLocked).isFalse()
        }

        @Test
        fun `lock should set isLocked to true`() {
            // given
            val row = QwixxRow.ascendingRow()

            // when
            val lockedRow = row.lock()

            // then
            assertThat(lockedRow.isLocked).isTrue()
        }

        @Test
        fun `canBeLocked should return true when row has 5+ marks and last value is marked`() {
            // given
            val row = QwixxRow.ascendingRow()
                .mark(2).mark(3).mark(4).mark(5).mark(12)

            // then
            assertThat(row.canBeLocked()).isTrue()
        }

        @Test
        fun `canBeLocked should return false when row has less than 5 marks`() {
            // given
            val row = QwixxRow.ascendingRow()
                .mark(2).mark(3).mark(4).mark(12)

            // then
            assertThat(row.canBeLocked()).isFalse()
        }

        @Test
        fun `canBeLocked should return false when last value is not marked`() {
            // given
            val row = QwixxRow.ascendingRow()
                .mark(2).mark(3).mark(4).mark(5).mark(6)

            // then
            assertThat(row.canBeLocked()).isFalse()
        }

        @ParameterizedTest
        @CsvSource(
            "0, 0",
            "1, 1",
            "2, 3",
            "3, 6",
            "4, 10",
            "5, 15",
            "6, 21"
        )
        fun `calculateScore should return correct score based on marked count`(markedCount: Int, expectedScore: Int) {
            // given
            val values = (2..12).toList()
            val markedPositions = values.take(markedCount).toSet()
            val row = QwixxRow(values, markedPositions)

            // when
            val score = row.calculateScore()

            // then
            assertThat(score).isEqualTo(expectedScore)
        }

        @Test
        fun `getRightmostMarkedIndex should return correct index`() {
            // given
            val row = QwixxRow.ascendingRow()
                .mark(2).mark(5).mark(8)

            // when
            val index = row.getRightmostMarkedIndex()

            // then
            assertThat(index).isEqualTo(6) // 8 is at index 6 in the list (2..12)
        }

        @Test
        fun `getRightmostMarkedIndex should return -1 for empty row`() {
            // given
            val row = QwixxRow.ascendingRow()

            // when
            val index = row.getRightmostMarkedIndex()

            // then
            assertThat(index).isEqualTo(-1)
        }

        @Test
        fun `getAvailableValues should return values after rightmost marked value`() {
            // given
            val row = QwixxRow.ascendingRow()
                .mark(2).mark(5)

            // when
            val availableValues = row.getAvailableValues()

            // then
            assertThat(availableValues).containsExactly(6, 7, 8, 9, 10, 11, 12)
        }

        @Test
        fun `getAvailableValues should return all values for empty row`() {
            // given
            val row = QwixxRow.ascendingRow()

            // when
            val availableValues = row.getAvailableValues()

            // then
            assertThat(availableValues).containsExactly(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        }
    }

    @Nested
    @DisplayName("ScoreSheet Tests")
    inner class ScoreSheetClassTests {

        @Test
        fun `default constructor should create scoresheet with default values`() {
            // when
            val scoreSheet = ScoreSheet()

            // then
            assertThat(scoreSheet.redRow.values).isEqualTo((2..12).toList())
            assertThat(scoreSheet.yellowRow.values).isEqualTo((2..12).toList())
            assertThat(scoreSheet.greenRow.values).isEqualTo((12 downTo 2).toList())
            assertThat(scoreSheet.blueRow.values).isEqualTo((12 downTo 2).toList())
            assertThat(scoreSheet.penalties).isEqualTo(0)
        }

        @Test
        fun `markNumber should mark number in specified row`() {
            // given
            val scoreSheet = ScoreSheet()

            // when
            val updatedSheet = scoreSheet.markNumber(RowColor.RED, 5)

            // then
            assertThat(updatedSheet).isNotNull
            assertThat(updatedSheet!!.redRow.markedPositions).contains(5)
        }

        @Test
        fun `markNumber should return null for invalid move`() {
            // given
            val scoreSheet = ScoreSheet()
                .markNumber(RowColor.RED, 5)

            // when
            val result = scoreSheet?.markNumber(RowColor.RED, 4)

            // then
            assertThat(result).isNull()
        }

        @Test
        fun `isValidMove should return true for valid move`() {
            // given
            val scoreSheet = ScoreSheet()

            // then
            assertThat(scoreSheet.isValidMove(RowColor.RED, 5)).isTrue()
        }

        @Test
        fun `isValidMove should return false for invalid move`() {
            // given
            val scoreSheet = ScoreSheet()
                .markNumber(RowColor.RED, 5)

            // then
            assertThat(scoreSheet?.isValidMove(RowColor.RED, 4)).isFalse()
        }

        @Test
        fun `isValidMove should return false for locked row`() {
            // given
            val scoreSheet = ScoreSheet()
                .lockRow(RowColor.RED)

            // then
            assertThat(scoreSheet.isValidMove(RowColor.RED, 5)).isFalse()
        }

        @Test
        fun `getValidMoves should return valid moves for active player`() {
            // given
            val scoreSheet = ScoreSheet()
            val diceRoll = mapOf(
                DiceColor.WHITE_1 to 2,
                DiceColor.WHITE_2 to 3,
                DiceColor.RED to 4,
                DiceColor.YELLOW to 5,
                DiceColor.GREEN to 6,
                DiceColor.BLUE to 1
            )

            // when
            val validMoves = scoreSheet.getValidMoves(diceRoll, true)

            // then
            assertThat(validMoves).isNotEmpty
            // Check that combinations include white dice + colored dice
            assertThat(validMoves.any { it.value == 6 && RowColor.RED in it.availableForRows }).isTrue() // WHITE_1(2) + RED(4)
            assertThat(validMoves.any { it.value == 7 && RowColor.YELLOW in it.availableForRows }).isTrue() // WHITE_1(2) + YELLOW(5)
        }

        @Test
        fun `getValidMoves should return only white dice combinations for non-active player`() {
            // given
            val scoreSheet = ScoreSheet()
            val diceRoll = mapOf(
                DiceColor.WHITE_1 to 2,
                DiceColor.WHITE_2 to 3,
                DiceColor.RED to 4,
                DiceColor.YELLOW to 5,
                DiceColor.GREEN to 6,
                DiceColor.BLUE to 1
            )

            // when
            val validMoves = scoreSheet.getValidMoves(diceRoll, false)

            // then
            assertThat(validMoves).hasSize(1)
            assertThat(validMoves[0].value).isEqualTo(5) // WHITE_1(2) + WHITE_2(3)
            assertThat(validMoves[0].availableForRows).containsExactlyInAnyOrder(
                RowColor.RED, RowColor.YELLOW, RowColor.GREEN, RowColor.BLUE
            )
        }

        @Test
        fun `addPenalty should increment penalty count`() {
            // given
            val scoreSheet = ScoreSheet()

            // when
            val updatedSheet = scoreSheet.addPenalty()

            // then
            assertThat(updatedSheet.penalties).isEqualTo(1)
        }

        @Test
        fun `lockRow should lock specified row`() {
            // given
            val scoreSheet = ScoreSheet()

            // when
            val updatedSheet = scoreSheet.lockRow(RowColor.RED)

            // then
            assertThat(updatedSheet.redRow.isLocked).isTrue()
        }

        @Test
        fun `canLockRow should return true when row can be locked`() {
            // given
            val redRow = QwixxRow.ascendingRow()
                .mark(2).mark(3).mark(4).mark(5).mark(12)
            val scoreSheet = ScoreSheet(redRow = redRow)

            // then
            assertThat(scoreSheet.canLockRow(RowColor.RED)).isTrue()
        }

        @Test
        fun `canLockRow should return false when row cannot be locked`() {
            // given
            val scoreSheet = ScoreSheet()

            // then
            assertThat(scoreSheet.canLockRow(RowColor.RED)).isFalse()
        }

        @Test
        fun `getMarkedCount should return correct count`() {
            // given
            val redRow = QwixxRow.ascendingRow()
                .mark(2).mark(3).mark(4)
            val scoreSheet = ScoreSheet(redRow = redRow)

            // then
            assertThat(scoreSheet.getMarkedCount(RowColor.RED)).isEqualTo(3)
        }

        @Test
        fun `isRowLocked should return true for locked row`() {
            // given
            val scoreSheet = ScoreSheet()
                .lockRow(RowColor.RED)

            // then
            assertThat(scoreSheet.isRowLocked(RowColor.RED)).isTrue()
        }

        @Test
        fun `isRowLocked should return false for unlocked row`() {
            // given
            val scoreSheet = ScoreSheet()

            // then
            assertThat(scoreSheet.isRowLocked(RowColor.RED)).isFalse()
        }

        @Test
        fun `calculateScore should return correct total score`() {
            // given
            val redRow = QwixxRow.ascendingRow().mark(2).mark(3) // 3 points
            val yellowRow = QwixxRow.ascendingRow().mark(2).mark(3).mark(4) // 6 points
            val greenRow = QwixxRow.descendingRow().mark(12).mark(11) // 3 points
            val blueRow = QwixxRow.descendingRow().mark(12) // 1 point
            val scoreSheet = ScoreSheet(
                redRow = redRow,
                yellowRow = yellowRow,
                greenRow = greenRow,
                blueRow = blueRow,
                penalties = 1 // -5 points
            )

            // when
            val score = scoreSheet.calculateScore()

            // then
            assertThat(score).isEqualTo(3 + 6 + 3 + 1 - 5) // 8 points
        }

        @Test
        fun `getScoreBreakdown should return detailed score breakdown`() {
            // given
            val redRow = QwixxRow.ascendingRow().mark(2).mark(3) // 3 points
            val yellowRow = QwixxRow.ascendingRow().mark(2).mark(3).mark(4) // 6 points
            val greenRow = QwixxRow.descendingRow().mark(12).mark(11) // 3 points
            val blueRow = QwixxRow.descendingRow().mark(12) // 1 point
            val scoreSheet = ScoreSheet(
                redRow = redRow,
                yellowRow = yellowRow,
                greenRow = greenRow,
                blueRow = blueRow,
                penalties = 1 // -5 points
            )

            // when
            val breakdown = scoreSheet.getScoreBreakdown()

            // then
            assertThat(breakdown.redScore).isEqualTo(3)
            assertThat(breakdown.yellowScore).isEqualTo(6)
            assertThat(breakdown.greenScore).isEqualTo(3)
            assertThat(breakdown.blueScore).isEqualTo(1)
            assertThat(breakdown.penaltyScore).isEqualTo(5)
            assertThat(breakdown.totalScore).isEqualTo(8)
        }

        @Test
        fun `shouldEndGame should return true when 4 penalties are reached`() {
            // given
            val scoreSheet = ScoreSheet(penalties = 4)

            // then
            assertThat(scoreSheet.shouldEndGame()).isTrue()
        }

        @Test
        fun `shouldEndGame should return true when 2 rows are locked`() {
            // given
            val redRow = QwixxRow.ascendingRow().lock()
            val yellowRow = QwixxRow.ascendingRow().lock()
            val scoreSheet = ScoreSheet(redRow = redRow, yellowRow = yellowRow)

            // then
            assertThat(scoreSheet.shouldEndGame()).isTrue()
        }

        @Test
        fun `shouldEndGame should return false when less than 4 penalties and less than 2 locked rows`() {
            // given
            val redRow = QwixxRow.ascendingRow().lock()
            val scoreSheet = ScoreSheet(redRow = redRow, penalties = 3)

            // then
            assertThat(scoreSheet.shouldEndGame()).isFalse()
        }
    }

    @Nested
    @DisplayName("ScoreBreakdown Tests")
    inner class ScoreBreakdownTests {

        @Test
        fun `should correctly initialize ScoreBreakdown`() {
            // when
            val breakdown = ScoreBreakdown(
                redScore = 10,
                yellowScore = 15,
                greenScore = 21,
                blueScore = 6,
                penaltyScore = 10,
                totalScore = 42
            )

            // then
            assertThat(breakdown.redScore).isEqualTo(10)
            assertThat(breakdown.yellowScore).isEqualTo(15)
            assertThat(breakdown.greenScore).isEqualTo(21)
            assertThat(breakdown.blueScore).isEqualTo(6)
            assertThat(breakdown.penaltyScore).isEqualTo(10)
            assertThat(breakdown.totalScore).isEqualTo(42)
        }
    }
}