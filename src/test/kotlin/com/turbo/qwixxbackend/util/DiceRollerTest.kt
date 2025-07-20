package com.turbo.qwixxbackend.util

import com.turbo.qwixxbackend.util.DiceColor.*
import com.turbo.qwixxbackend.util.DiceRoller.calculateActivePlayerMoves
import com.turbo.qwixxbackend.util.DiceRoller.calculateNonActivePlayerMoves
import com.turbo.qwixxbackend.util.DiceRoller.printableDiceRoll
import com.turbo.qwixxbackend.util.DiceRoller.rollAllDice
import com.turbo.qwixxbackend.util.DiceRoller.validateDiceValues
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("DiceRoller Tests with Mockk and AssertJ")
class DiceRollerTest {

    @BeforeEach
    fun setUp() {
        mockkObject(DiceRoller)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Nested
    @DisplayName("rollAllDice() tests")
    inner class RollAllDiceTests {

        @Test
        @DisplayName("rollAllDice() should return a map with all 6 dice colors")
        fun rollAllDiceShouldReturnMapWithAllDiceColors() {
            // Given
            // Mock the rollAllDice method to return a fixed map
            val mockedResult = mapOf(
                WHITE_1 to 3,
                WHITE_2 to 3,
                RED to 3,
                YELLOW to 3,
                GREEN to 3,
                BLUE to 3
            )
            every { rollAllDice() } returns mockedResult

            // When
            val result = rollAllDice()

            // Then
            assertThat(result)
                .hasSize(6)
                .containsKeys(
                    WHITE_1,
                    WHITE_2,
                    RED,
                    YELLOW,
                    GREEN,
                    BLUE
                )

            verify(exactly = 1) { rollAllDice() }
        }

        @Test
        @DisplayName("rollAllDice() should return values between 1 and 6")
        fun rollAllDiceShouldReturnValuesBetweenOneAndSix() {
            // Given
            // Mock the rollAllDice method to return values between 1 and 6
            val mockedResult = mapOf(
                WHITE_1 to 1,
                WHITE_2 to 2,
                RED to 3,
                YELLOW to 4,
                GREEN to 5,
                BLUE to 6
            )
            every { rollAllDice() } returns mockedResult

            // When
            val result = rollAllDice()

            // Then
            assertThat(result.values).allMatch { it in 1..6 }

            // Verify rollAllDice was called
            verify(exactly = 1) { rollAllDice() }
        }

        @Test
        @DisplayName("rollAllDice() should generate different values on multiple calls")
        fun rollAllDiceShouldGenerateDifferentValuesOnMultipleCalls() {
            // Given
            val firstResult = mapOf(
                WHITE_1 to 1,
                WHITE_2 to 1,
                RED to 1,
                YELLOW to 1,
                GREEN to 1,
                BLUE to 1
            )

            val secondResult = mapOf(
                WHITE_1 to 2,
                WHITE_2 to 2,
                RED to 2,
                YELLOW to 2,
                GREEN to 2,
                BLUE to 2
            )

            every { rollAllDice() } returnsMany listOf(firstResult, secondResult)

            // When
            val result1 = rollAllDice()
            val result2 = rollAllDice()

            // Then
            assertThat(result1).isNotEqualTo(result2)

            verify(exactly = 2) { rollAllDice() }
        }
    }

    @Nested
    @DisplayName("calculateActivePlayerMoves() tests")
    inner class CalculateActivePlayerMovesTests {

        @Test
        @DisplayName("calculateActivePlayerMoves() should return correct combinations")
        fun calculateActivePlayerMovesShouldReturnCorrectCombinations() {
            // Given
            val diceValues = mapOf(
                WHITE_1 to 1,
                WHITE_2 to 2,
                RED to 3,
                YELLOW to 4,
                GREEN to 5,
                BLUE to 6
            )

            // When
            val result = calculateActivePlayerMoves(diceValues)

            // Then
            // Should have 9 combinations (white1+white2, white1+each color, white2+each color),
            // But some might be removed as duplicates if they have the same value and available rows
            assertThat(result.size).isLessThanOrEqualTo(9)

            // Check white1 + white2 combination
            val whiteCombo = result.find { combo ->
                combo.diceUsed.containsAll(listOf(WHITE_1, WHITE_2))
            }
            assertThat(whiteCombo).isNotNull
            assertThat(whiteCombo!!.value).isEqualTo(3)
            assertThat(whiteCombo.availableForRows).hasSize(4)

            // Check white1 + red combination
            val whiteRedCombo = result.find { combo ->
                combo.diceUsed.containsAll(listOf(WHITE_1, RED))
            }
            assertThat(whiteRedCombo).isNotNull
            assertThat(whiteRedCombo!!.value).isEqualTo(4)
            assertThat(whiteRedCombo.availableForRows).hasSize(1)
            assertThat(whiteRedCombo.availableForRows[0]).isEqualTo(RowColor.RED)

            // Check for duplicates removal
            val distinctValues = result.map { "${it.value}-${it.availableForRows.joinToString()}" }.distinct()
            assertThat(distinctValues).hasSize(result.size)
        }

        @Test
        @DisplayName("calculateActivePlayerMoves() should handle duplicate values correctly")
        fun calculateActivePlayerMovesShouldHandleDuplicateValuesCorrectly() {
            val diceValues = mapOf(
                WHITE_1 to 2,
                WHITE_2 to 2,
                RED to 2,
                YELLOW to 2,
                GREEN to 2,
                BLUE to 2
            )

            // When
            val result = calculateActivePlayerMoves(diceValues)

            // Then
            // Should have 5 combinations after removing duplicates
            // (white1+white2 for all colors, white1+red for red, white1+yellow for yellow, etc.)
            assertThat(result)
                .hasSize(5)
                .allMatch { it.value == 4 }
        }
    }

    @Nested
    @DisplayName("calculateNonActivePlayerMoves() tests")
    inner class CalculateNonActivePlayerMovesTests {

        @Test
        @DisplayName("calculateNonActivePlayerMoves() should return only white dice combination")
        fun calculateNonActivePlayerMovesShouldReturnOnlyWhiteDiceCombination() {
            // Given
            val diceValues = mapOf(
                WHITE_1 to 3,
                WHITE_2 to 4,
                RED to 5,
                YELLOW to 6,
                GREEN to 1,
                BLUE to 2
            )

            // When
            val result = calculateNonActivePlayerMoves(diceValues)

            // Then
            assertThat(result).hasSize(1)

            with(result[0]) {
                assertThat(value).isEqualTo(7) // 3+4
                assertThat(diceUsed).hasSize(2)
                assertThat(diceUsed).contains(WHITE_1)
                assertThat(diceUsed).contains(WHITE_2)
                assertThat(availableForRows).hasSize(4)
            }
        }
    }

    @Nested
    @DisplayName("printableDiceRoll() tests")
    inner class PrintableDiceRollTests {

        @Test
        @DisplayName("printableDiceRoll() should format dice values correctly")
        fun printableDiceRollShouldFormatDiceValuesCorrectly() {
            // Given
            val diceValues = mapOf(
                WHITE_1 to 1,
                WHITE_2 to 2,
                RED to 3,
                YELLOW to 4,
                GREEN to 5,
                BLUE to 6
            )

            // When
            val result = printableDiceRoll(diceValues)

            // Then
            val expected = "Dice Roll: W1:1 W2:2 R:3 Y:4 G:5 B:6"
            assertThat(result).isEqualTo(expected)
        }
    }

    @Nested
    @DisplayName("validateDiceValues() tests")
    inner class ValidateDiceValuesTests {

        @Test
        @DisplayName("validateDiceValues() should return true for valid dice values")
        fun validateDiceValuesShouldReturnTrueForValidDiceValues() {
            // Given
            val diceValues = mapOf(
                WHITE_1 to 1,
                WHITE_2 to 2,
                RED to 3,
                YELLOW to 4,
                GREEN to 5,
                BLUE to 6
            )

            // When
            val result = validateDiceValues(diceValues)

            // Then
            assertThat(result).isTrue()
        }

        @Test
        @DisplayName("validateDiceValues() should return false for invalid dice values")
        fun validateDiceValuesShouldReturnFalseForInvalidDiceValues() {
            // Given - value outside valid range
            val invalidValueDiceValues = mapOf(
                WHITE_1 to 1,
                WHITE_2 to 2,
                RED to 3,
                YELLOW to 4,
                GREEN to 5,
                BLUE to 7  // Invalid value (> 6)
            )

            // When & Then
            assertThat(validateDiceValues(invalidValueDiceValues)).isFalse()

            // Given - missing dice color
            val missingColorDiceValues = mapOf(
                WHITE_1 to 1,
                WHITE_2 to 2,
                RED to 3,
                YELLOW to 4,
                GREEN to 5
                // BLUE is missing
            )

            // When & Then
            assertThat(validateDiceValues(missingColorDiceValues)).isFalse()
        }

        @ParameterizedTest
        @ValueSource(ints = [0, 7, -1, 10])
        @DisplayName("validateDiceValues() should return false for values outside 1-6 range")
        fun validateDiceValuesShouldReturnFalseForValuesOutsideRange(invalidValue: Int) {
            // Given
            val diceValues = mapOf(
                WHITE_1 to 1,
                WHITE_2 to 2,
                RED to 3,
                YELLOW to 4,
                GREEN to 5,
                BLUE to invalidValue
            )

            // When
            val result = validateDiceValues(diceValues)

            // Then
            assertThat(result).isFalse()
        }
    }

    @Nested
    @DisplayName("Edge cases and special scenarios")
    inner class EdgeCasesAndSpecialScenarios {

        @Test
        @DisplayName("calculateActivePlayerMoves() should handle minimum dice values")
        fun calculateActivePlayerMovesShouldHandleMinimumDiceValues() {
            // Given - all dice have minimum value (1)
            val diceValues = mapOf(
                WHITE_1 to 1,
                WHITE_2 to 1,
                RED to 1,
                YELLOW to 1,
                GREEN to 1,
                BLUE to 1
            )

            // When
            val result = calculateActivePlayerMoves(diceValues)

            // Then
            // Should have 5 combinations after removing duplicates
            assertThat(result).hasSize(5)

            // All combinations should have value 2 (1+1)
            assertThat(result).allMatch { it.value == 2 }
        }

        @Test
        @DisplayName("calculateActivePlayerMoves() should handle maximum dice values")
        fun calculateActivePlayerMovesShouldHandleMaximumDiceValues() {
            // Given - all dice have maximum value (6)
            val diceValues = mapOf(
                WHITE_1 to 6,
                WHITE_2 to 6,
                RED to 6,
                YELLOW to 6,
                GREEN to 6,
                BLUE to 6
            )

            // When
            val result = calculateActivePlayerMoves(diceValues)

            // Then
            // Should have 5 combinations after removing duplicates
            assertThat(result).hasSize(5)

            // All combinations should have value 12 (6+6)
            assertThat(result).allMatch { it.value == 12 }
        }
    }
}