package com.turbo.qwixxbackend.entity

import com.turbo.qwixxbackend.entity.statemachine.WaitingForDiceRollState
import com.turbo.qwixxbackend.util.DiceColor
import com.turbo.qwixxbackend.util.RowColor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class GameTest {

    @Nested
    @DisplayName("Move Execution Tests")
    inner class MoveExecutionTests {

        @Test
        @DisplayName("Should roll dice and transition to white dice phase")
        fun shouldRollDiceAndTransitionToWhiteDicePhase() {
            // Given
            val game = Game.createGame(listOf("Player1", "Player2"))
            assertThat(game.getCurrentTurnState()).isEqualTo(TurnState.WAITING_FOR_DICE_ROLL)
            
            // When
            val result = game.rollDice()
            
            // Then
            assertThat(result).isTrue()
            assertThat(game.getCurrentTurnState()).isEqualTo(TurnState.WHITE_DICE_PHASE)
            assertThat(game.diceValues).isNotNull()
            assertThat(game.diceValues!!.size).isEqualTo(6) // 2 white dice + 4 colored dice
        }

        @Test
        @DisplayName("Should not roll dice when not in waiting for dice roll state")
        fun shouldNotRollDiceWhenNotInWaitingForDiceRollState() {
            // Given
            val game = Game.createGame(listOf("Player1", "Player2"))
            game.rollDice() // Move to white dice phase
            assertThat(game.getCurrentTurnState()).isEqualTo(TurnState.WHITE_DICE_PHASE)
            
            // When
            val result = game.rollDice()
            
            // Then
            assertThat(result).isFalse()
            assertThat(game.getCurrentTurnState()).isEqualTo(TurnState.WHITE_DICE_PHASE)
        }

        @Test
        @DisplayName("Should make valid white dice move")
        fun shouldMakeValidWhiteDiceMove() {
            // Given
            val game = Game.createGame(listOf("Player1", "Player2"))
            game.rollDice()
            assertThat(game.getCurrentTurnState()).isEqualTo(TurnState.WHITE_DICE_PHASE)
            
            // Get white dice values
            val whiteDice1 = game.diceValues!![DiceColor.WHITE_1]!!
            val whiteDice2 = game.diceValues!![DiceColor.WHITE_2]!!
            val sum = whiteDice1 + whiteDice2
            
            // When - try to mark the sum in the red row
            val result = game.makeWhiteDiceMove("player_0", RowColor.RED)
            
            // Then
            assertThat(result).isTrue()
            assertThat(game.playersWhoMovedThisTurn).contains("player_0")
            assertThat(game.players[0].scoreSheet.redRow.markedPositions).contains(sum)
        }

        @Test
        @DisplayName("Should not make invalid white dice move")
        fun shouldNotMakeInvalidWhiteDiceMove() {
            // Given
            val game = Game.createGame(listOf("Player1", "Player2"))
            game.rollDice()
            
            // First make a valid move to mark a number
            val whiteDice1 = game.diceValues!![DiceColor.WHITE_1]!!
            val whiteDice2 = game.diceValues!![DiceColor.WHITE_2]!!
            val sum = whiteDice1 + whiteDice2
            game.makeWhiteDiceMove("player_0", RowColor.RED)
            
            // When - try to make the same move again (should be invalid)
            val result = game.makeWhiteDiceMove("player_0", RowColor.RED)
            
            // Then
            assertThat(result).isFalse()
        }

        @Test
        @DisplayName("Should pass white dice move")
        fun shouldPassWhiteDiceMove() {
            // Given
            val game = Game.createGame(listOf("Player1", "Player2"))
            game.rollDice()
            
            // When
            val result = game.passWhiteDiceMove("player_0")
            
            // Then
            assertThat(result).isTrue()
            assertThat(game.playersWhoPassedWhiteDice).contains("player_0")
        }

        @Test
        @DisplayName("Should transition to active player phase when all players have moved or passed in white dice phase")
        fun shouldTransitionToActivePlayerPhaseWhenAllPlayersHaveMovedOrPassed() {
            // Given
            val game = Game.createGame(listOf("Player1", "Player2"))
            game.rollDice()
            
            // When
            game.passWhiteDiceMove("player_0")
            game.passWhiteDiceMove("player_1")
            
            // Then
            assertThat(game.whiteDicePhaseCompleted).isTrue()
            assertThat(game.getCurrentTurnState()).isEqualTo(TurnState.ACTIVE_PLAYER_PHASE)
        }

        @Test
        @DisplayName("Should make valid active player move")
        fun shouldMakeValidActivePlayerMove() {
            // Given
            val game = Game.createGame(listOf("Player1", "Player2"))
            game.rollDice()
            game.passWhiteDiceMove("player_0")
            game.passWhiteDiceMove("player_1")
            assertThat(game.getCurrentTurnState()).isEqualTo(TurnState.ACTIVE_PLAYER_PHASE)
            
            // When - active player makes a move
            val result = game.makeActivePlayerMove("player_0", RowColor.RED)
            
            // Then
            assertThat(result).isTrue()
            assertThat(game.playersWhoMovedThisTurn).contains("player_0")
            // We don't check the exact number marked because it depends on the dice roll
            assertThat(game.players[0].scoreSheet.redRow.markedPositions).isNotEmpty()
            // The state should be TURN_ENDED, not WAITING_FOR_DICE_ROLL
            assertThat(game.getCurrentTurnState()).isEqualTo(TurnState.TURN_ENDED)
        }

        @Test
        @DisplayName("Should not make active player move when not current player")
        fun shouldNotMakeActivePlayerMoveWhenNotCurrentPlayer() {
            // Given
            val game = Game.createGame(listOf("Player1", "Player2"))
            game.rollDice()
            game.passWhiteDiceMove("player_0")
            game.passWhiteDiceMove("player_1")
            assertThat(game.getCurrentTurnState()).isEqualTo(TurnState.ACTIVE_PLAYER_PHASE)
            
            // When - non-active player tries to make a move
            val result = game.makeActivePlayerMove("player_1", RowColor.RED)
            
            // Then
            assertThat(result).isFalse()
            assertThat(game.playersWhoMovedThisTurn).doesNotContain("player_1")
        }

        @Test
        @DisplayName("Should pass active player move and end turn")
        fun shouldPassActivePlayerMoveAndEndTurn() {
            // Given
            val game = Game.createGame(listOf("Player1", "Player2"))
            game.rollDice()
            game.passWhiteDiceMove("player_0")
            game.passWhiteDiceMove("player_1")
            assertThat(game.getCurrentTurnState()).isEqualTo(TurnState.ACTIVE_PLAYER_PHASE)
            
            // When
            val result = game.passActivePlayerMove("player_0")
            
            // Then
            assertThat(result).isTrue()
            // The state should be TURN_ENDED, not WAITING_FOR_DICE_ROLL
            assertThat(game.getCurrentTurnState()).isEqualTo(TurnState.TURN_ENDED)
            // We don't check the current player index as it might not change immediately
        }
        
        @Test
        @DisplayName("Should allow active player to use white and colored dice combination")
        fun shouldAllowActivePlayerToUseWhiteAndColoredDiceCombination() {
            // Given
            val game = Game.createGame(listOf("Player1", "Player2"))
            game.rollDice()
            
            // Force specific dice values for testing
            val diceValues = mapOf(
                DiceColor.WHITE_1 to 3,
                DiceColor.WHITE_2 to 4,
                DiceColor.RED to 5,
                DiceColor.YELLOW to 2,
                DiceColor.GREEN to 6,
                DiceColor.BLUE to 1
            )
            // Use reflection to set dice values
            val diceValuesField = game::class.java.getDeclaredField("diceValues")
            diceValuesField.isAccessible = true
            diceValuesField.set(game, diceValues)
            
            // Move to active player phase
            game.passWhiteDiceMove("player_0")
            game.passWhiteDiceMove("player_1")
            assertThat(game.getCurrentTurnState()).isEqualTo(TurnState.ACTIVE_PLAYER_PHASE)
            
            // When - active player explicitly chooses WHITE_1 + RED combination
            val result = game.makeActivePlayerMove("player_0", RowColor.RED, DiceColor.WHITE_1)
            
            // Then
            assertThat(result).isTrue()
            assertThat(game.playersWhoMovedThisTurn).contains("player_0")
            
            // Verify that the WHITE_1 + RED combination was used (3 + 5 = 8)
            val markedPositions = game.players[0].scoreSheet.redRow.markedPositions
            assertThat(markedPositions).contains(8) // WHITE_1 (3) + RED (5) = 8
            
            // Create a second game to test WHITE_2 + RED combination
            val game2 = Game.createGame(listOf("Player1", "Player2"))
            game2.rollDice()
            
            // Set the same dice values
            diceValuesField.set(game2, diceValues)
            
            // Move to active player phase
            game2.passWhiteDiceMove("player_0")
            game2.passWhiteDiceMove("player_1")
            
            // When - active player explicitly chooses WHITE_2 + RED combination
            val result2 = game2.makeActivePlayerMove("player_0", RowColor.RED, DiceColor.WHITE_2)
            
            // Then
            assertThat(result2).isTrue()
            
            // Verify that the WHITE_2 + RED combination was used (4 + 5 = 9)
            val markedPositions2 = game2.players[0].scoreSheet.redRow.markedPositions
            assertThat(markedPositions2).contains(9) // WHITE_2 (4) + RED (5) = 9
            
            assertThat(game2.getCurrentTurnState()).isEqualTo(TurnState.TURN_ENDED)
        }
        
        @Test
        @DisplayName("Should allow active player to choose specific white dice for combination")
        fun shouldAllowActivePlayerToChooseSpecificWhiteDice() {
            // Given
            val game = Game.createGame(listOf("Player1", "Player2"))
            game.rollDice()
            
            // Force specific dice values for testing
            val diceValues = mapOf(
                DiceColor.WHITE_1 to 3,
                DiceColor.WHITE_2 to 4,
                DiceColor.RED to 5,
                DiceColor.YELLOW to 2,
                DiceColor.GREEN to 6,
                DiceColor.BLUE to 1
            )
            // Use reflection to set dice values
            val diceValuesField = game::class.java.getDeclaredField("diceValues")
            diceValuesField.isAccessible = true
            diceValuesField.set(game, diceValues)
            
            // Move to active player phase
            game.passWhiteDiceMove("player_0")
            game.passWhiteDiceMove("player_1")
            assertThat(game.getCurrentTurnState()).isEqualTo(TurnState.ACTIVE_PLAYER_PHASE)
            
            // When - active player makes a move in the red row with WHITE_1
            val result1 = game.makeActivePlayerMove("player_0", RowColor.RED, DiceColor.WHITE_1)
            
            // Then
            assertThat(result1).isTrue()
            
            // Verify that the WHITE_1 + RED combination was used (3 + 5 = 8)
            val markedPositions = game.players[0].scoreSheet.redRow.markedPositions
            assertThat(markedPositions).contains(8) // WHITE_1 (3) + RED (5) = 8
            
            // Reset the game for the second test
            val game2 = Game.createGame(listOf("Player1", "Player2"))
            game2.rollDice()
            
            // Set the same dice values
            diceValuesField.set(game2, diceValues)
            
            // Move to active player phase
            game2.passWhiteDiceMove("player_0")
            game2.passWhiteDiceMove("player_1")
            
            // When - active player makes a move in the red row with WHITE_2
            val result2 = game2.makeActivePlayerMove("player_0", RowColor.RED, DiceColor.WHITE_2)
            
            // Then
            assertThat(result2).isTrue()
            
            // Verify that the WHITE_2 + RED combination was used (4 + 5 = 9)
            val markedPositions2 = game2.players[0].scoreSheet.redRow.markedPositions
            assertThat(markedPositions2).contains(9) // WHITE_2 (4) + RED (5) = 9
        }

        @Test
        @DisplayName("Should complete a full turn cycle")
        fun shouldCompleteFullTurnCycle() {
            // Given
            val game = Game.createGame(listOf("Player1", "Player2"))
            
            // Player 1's turn
            assertThat(game.currentPlayerIndex).isEqualTo(0)
            assertThat(game.getCurrentTurnState()).isEqualTo(TurnState.WAITING_FOR_DICE_ROLL)
            
            // Roll dice
            game.rollDice()
            assertThat(game.getCurrentTurnState()).isEqualTo(TurnState.WHITE_DICE_PHASE)
            
            // Both players pass white dice phase
            game.passWhiteDiceMove("player_0")
            game.passWhiteDiceMove("player_1")
            assertThat(game.getCurrentTurnState()).isEqualTo(TurnState.ACTIVE_PLAYER_PHASE)
            
            // Active player passes
            game.passActivePlayerMove("player_0")
            assertThat(game.getCurrentTurnState()).isEqualTo(TurnState.TURN_ENDED)
            
            // End turn to move to next player
            game.endTurn()
            
            // Player 2's turn
            assertThat(game.currentPlayerIndex).isEqualTo(1)
            assertThat(game.getCurrentTurnState()).isEqualTo(TurnState.WAITING_FOR_DICE_ROLL)
            
            // Roll dice
            game.rollDice()
            assertThat(game.getCurrentTurnState()).isEqualTo(TurnState.WHITE_DICE_PHASE)
            
            // Both players pass white dice phase
            game.passWhiteDiceMove("player_0")
            game.passWhiteDiceMove("player_1")
            assertThat(game.getCurrentTurnState()).isEqualTo(TurnState.ACTIVE_PLAYER_PHASE)
            
            // Active player passes
            game.passActivePlayerMove("player_1")
            assertThat(game.getCurrentTurnState()).isEqualTo(TurnState.TURN_ENDED)
            
            // End turn to move to next player
            game.endTurn()
            
            // Back to Player 1's turn
            assertThat(game.currentPlayerIndex).isEqualTo(0)
            assertThat(game.getCurrentTurnState()).isEqualTo(TurnState.WAITING_FOR_DICE_ROLL)
        }
    }

    @Nested
    @DisplayName("Game Creation Tests")
    inner class GameCreationTests {

        @Test
        @DisplayName("Should create a game with one player")
        fun shouldCreateGameWithOnePlayer() {
            // When
            val game = Game.createGame(listOf("Player1"))

            // Then
            assertThat(game.players).hasSize(1)
            assertThat(game.players[0].name).isEqualTo("Player1")
            assertThat(game.players[0].isHost).isTrue()
            assertThat(game.gameLifecycle).isEqualTo(GameLifecycle.WAITING_FOR_PLAYERS)
            assertThat(game.currentPlayerIndex).isEqualTo(0)
            assertThat(game.diceValues).isNull()
            assertThat(game.lockedRows).isEmpty()
            assertThat(game.playersWhoMovedThisTurn).isEmpty()
            assertThat(game.playersWhoPassedWhiteDice).isEmpty()
            assertThat(game.whiteDicePhaseCompleted).isFalse()
            assertThat(game.finishedAt).isNull()
        }

        @Test
        @DisplayName("Should create a game with multiple players")
        fun shouldCreateGameWithMultiplePlayers() {
            // When
            val game = Game.createGame(listOf("Player1", "Player2", "Player3"))

            // Then
            assertThat(game.players).hasSize(3)
            assertThat(game.players.map { it.name }).containsExactly("Player1", "Player2", "Player3")
            assertThat(game.players[0].isHost).isTrue()
            assertThat(game.players[1].isHost).isFalse()
            assertThat(game.players[2].isHost).isFalse()
            assertThat(game.gameLifecycle).isEqualTo(GameLifecycle.IN_PROGRESS)
        }

        @Test
        @DisplayName("Should create a game with players and AI")
        fun shouldCreateGameWithPlayersAndAI() {
            // When
            val game = Game.createGame(listOf("Player1"), aiCount = 2)

            // Then
            assertThat(game.players).hasSize(3)
            assertThat(game.players[0].name).isEqualTo("Player1")
            assertThat(game.players[0].isAi).isFalse()
            assertThat(game.players[0].isHost).isTrue()
            
            assertThat(game.players[1].name).isEqualTo("AI Player 1")
            assertThat(game.players[1].isAi).isTrue()
            assertThat(game.players[1].isHost).isFalse()
            
            assertThat(game.players[2].name).isEqualTo("AI Player 2")
            assertThat(game.players[2].isAi).isTrue()
            assertThat(game.players[2].isHost).isFalse()
            
            assertThat(game.gameLifecycle).isEqualTo(GameLifecycle.IN_PROGRESS)
        }

        @Test
        @DisplayName("Should throw exception when creating game with no players")
        fun shouldThrowExceptionWhenCreatingGameWithNoPlayers() {
            // When/Then
            val exception = assertThrows<IllegalArgumentException> {
                Game.createGame(emptyList())
            }
            
            assertThat(exception.message).isEqualTo("At least one player required")
        }

        @Test
        @DisplayName("Should throw exception when creating game with too many players")
        fun shouldThrowExceptionWhenCreatingGameWithTooManyPlayers() {
            // When/Then
            val exception = assertThrows<IllegalArgumentException> {
                Game.createGame(listOf("Player1", "Player2", "Player3", "Player4", "Player5", "Player6"))
            }
            
            assertThat(exception.message).isEqualTo("Maximum 5 players allowed")
        }

        @Test
        @DisplayName("Should throw exception when creating game with too many players and AI")
        fun shouldThrowExceptionWhenCreatingGameWithTooManyPlayersAndAI() {
            // When/Then
            val exception = assertThrows<IllegalArgumentException> {
                Game.createGame(listOf("Player1", "Player2", "Player3"), aiCount = 3)
            }
            
            assertThat(exception.message).isEqualTo("Maximum 5 players allowed")
        }
        
        @Test
        @DisplayName("Should assign correct player IDs when creating a game")
        fun shouldAssignCorrectPlayerIdsWhenCreatingGame() {
            // When
            val game = Game.createGame(listOf("Player1", "Player2"), aiCount = 1)
            
            // Then
            assertThat(game.players).hasSize(3)
            assertThat(game.players[0].id).isEqualTo("player_0")
            assertThat(game.players[1].id).isEqualTo("player_1")
            assertThat(game.players[2].id).isEqualTo("ai_1")
        }
    }

    @Nested
    @DisplayName("Add Player Tests")
    inner class AddPlayerTests {

        @Test
        @DisplayName("Should add player to game in waiting state")
        fun shouldAddPlayerToGameInWaitingState() {
            // Given
            val game = Game.createGame(listOf("Player1"))
            assertThat(game.gameLifecycle).isEqualTo(GameLifecycle.WAITING_FOR_PLAYERS)
            
            // When
            val result = game.addPlayer("Player2")
            
            // Then
            assertThat(result).isTrue()
            assertThat(game.players).hasSize(2)
            assertThat(game.players[1].name).isEqualTo("Player2")
            assertThat(game.gameLifecycle).isEqualTo(GameLifecycle.IN_PROGRESS)
        }

        @Test
        @DisplayName("Should not add player to game not in waiting state")
        fun shouldNotAddPlayerToGameNotInWaitingState() {
            // Given
            val game = Game.createGame(listOf("Player1", "Player2"))
            assertThat(game.gameLifecycle).isEqualTo(GameLifecycle.IN_PROGRESS)
            
            // When
            val result = game.addPlayer("Player3")
            
            // Then
            assertThat(result).isFalse()
            assertThat(game.players).hasSize(2)
        }

        @Test
        @DisplayName("Should not add player when game already has maximum players")
        fun shouldNotAddPlayerWhenGameAlreadyHasMaximumPlayers() {
            // Given
            val game = Game.createGame(listOf("Player1", "Player2", "Player3", "Player4", "Player5"))
            assertThat(game.players).hasSize(5)
            
            // When
            val result = game.addPlayer("Player6")
            
            // Then
            assertThat(result).isFalse()
            assertThat(game.players).hasSize(5)
        }

        @Test
        @DisplayName("Should transition game state when second player is added")
        fun shouldTransitionGameStateWhenSecondPlayerIsAdded() {
            // Given
            val game = Game.createGame(listOf("Player1"))
            assertThat(game.gameLifecycle).isEqualTo(GameLifecycle.WAITING_FOR_PLAYERS)
            
            // When
            game.addPlayer("Player2")
            
            // Then
            assertThat(game.gameLifecycle).isEqualTo(GameLifecycle.IN_PROGRESS)
        }
    }

    @Nested
    @DisplayName("Game State Tests")
    inner class GameStateTests {

        @Test
        @DisplayName("Should initialize with WaitingForDiceRollState")
        fun shouldInitializeWithWaitingForDiceRollState() {
            // When
            val game = Game.createGame(listOf("Player1", "Player2"))
            
            // Then
            assertThat(game.getCurrentTurnState()).isEqualTo(TurnState.WAITING_FOR_DICE_ROLL)
        }

        @Test
        @DisplayName("Should have correct initial game lifecycle based on player count")
        fun shouldHaveCorrectInitialGameLifecycleBasedOnPlayerCount() {
            // When - One player
            val singlePlayerGame = Game.createGame(listOf("Player1"))
            
            // Then
            assertThat(singlePlayerGame.gameLifecycle).isEqualTo(GameLifecycle.WAITING_FOR_PLAYERS)
            
            // When - Multiple players
            val multiPlayerGame = Game.createGame(listOf("Player1", "Player2"))
            
            // Then
            assertThat(multiPlayerGame.gameLifecycle).isEqualTo(GameLifecycle.IN_PROGRESS)
        }
    }
}