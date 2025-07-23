package com.turbo.qwixxbackend.entity

import com.turbo.qwixxbackend.entity.statemachine.GameState
import com.turbo.qwixxbackend.entity.statemachine.WaitingForDiceRollState
import com.turbo.qwixxbackend.util.DiceColor
import com.turbo.qwixxbackend.util.DiceCombination
import com.turbo.qwixxbackend.util.DiceRoller
import com.turbo.qwixxbackend.util.RowColor
import java.time.LocalDateTime
import java.util.UUID.randomUUID

/**
 * Mutable Game class using a State Machine pattern
 */
class Game(
    val id: String = randomUUID().toString(),
    val players: MutableList<Player> = mutableListOf(),
    var currentPlayerIndex: Int = 0,
    var gameLifecycle: GameLifecycle = GameLifecycle.WAITING_FOR_PLAYERS,
    var diceValues: Map<DiceColor, Int>? = null,
    var lockedRows: MutableSet<RowColor> = mutableSetOf(),
    var playersWhoMovedThisTurn: MutableSet<String> = mutableSetOf(),
    var playersWhoPassedWhiteDice: MutableSet<String> = mutableSetOf(),
    var whiteDicePhaseCompleted: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var finishedAt: LocalDateTime? = null
) {

    private var currentState: GameState = WaitingForDiceRollState()

    companion object {
        /**
         * Creates a new game with given players and AI count
         */
        fun createGame(playerNames: List<String>, aiCount: Int = 0): Game {
            require(playerNames.isNotEmpty()) { "At least one player required" }
            require(playerNames.size + aiCount <= 5) { "Maximum 5 players allowed" }

            val game = Game()

            // Add human players
            playerNames.forEachIndexed { index, name ->
                game.players.add(
                    Player(
                        id = "player_${index}",
                        name = name,
                        isHost = index == 0
                    )
                )
            }

            // Add AI players
            repeat(aiCount) { aiIndex ->
                game.players.add(
                    Player(
                        id = "ai_${aiIndex + 1}",
                        name = "AI Player ${aiIndex + 1}",
                        isAi = true
                    )
                )
            }

            if (game.players.size >= 2) {
                game.gameLifecycle = GameLifecycle.IN_PROGRESS
            }

            return game
        }
    }

    /**
     * Adds a player to the game (if in waiting state)
     */
    fun addPlayer(playerName: String): Boolean {
        if (gameLifecycle != GameLifecycle.WAITING_FOR_PLAYERS || players.size >= 5) {
            return false
        }

        players.add(
            Player(
                id = "player_${players.size}",
                name = playerName
            )
        )

        if (players.size >= 2) {
            gameLifecycle = GameLifecycle.IN_PROGRESS
        }

        return true
    }

    /**
     * Rolls all dice to start a turn
     */
    fun rollDice(): Boolean {
        if (!currentState.canRollDice()) return false

        // Business logic
        diceValues = DiceRoller.rollAllDice()
        playersWhoMovedThisTurn.clear()
        playersWhoPassedWhiteDice.clear()
        whiteDicePhaseCompleted = false

        // State transition
        currentState = currentState.onRollDice(this)
        return true
    }

    /**
     * Makes a move using white dice (available to all players in white dice phase only)
     */
    fun makeWhiteDiceMove(playerId: String, rowColor: RowColor): Boolean {
        if (whiteDicePhaseCompleted) return false  // Phase closed!
        if (!currentState.canMakeWhiteDiceMove(playerId)) return false
        if (lockedRows.contains(rowColor)) return false

        // Business logic
        val player = getPlayer(playerId) ?: return false
        val diceSum = diceValues?.let { it[DiceColor.WHITE_1]!! + it[DiceColor.WHITE_2]!! } ?: return false
        val newScoreSheet = player.scoreSheet.markNumber(rowColor, diceSum) ?: return false

        updatePlayer(playerId, newScoreSheet)
        playersWhoMovedThisTurn.add(playerId)
        checkRowLocking(rowColor, newScoreSheet)
        checkGameEndConditions()

        // Check if the white dice phase should auto-complete
        checkIfWhiteDicePhaseComplete()

        // State transition (usually stays in same state)
        currentState = currentState.onWhiteDiceMove(this)
        return true
    }

    /**
     * Player passes on white dice move
     */
    fun passWhiteDiceMove(playerId: String): Boolean {
        if (whiteDicePhaseCompleted) return false  // Phase closed!
        if (!currentState.canMakeWhiteDiceMove(playerId)) return false

        playersWhoPassedWhiteDice.add(playerId)
        checkIfWhiteDicePhaseComplete()
        return true
    }

    /**
     * Makes a move as active player using white + colored dice
     */
    fun makeActivePlayerMove(playerId: String, rowColor: RowColor): Boolean {
        return makeActivePlayerMove(playerId, rowColor, null)
    }
    
    /**
     * Makes a move as active player using white + colored dice with specified white dice
     * @param playerId The ID of the player making the move
     * @param rowColor The color of the row to mark
     * @param whiteColor The specific white dice to use (WHITE_1 or WHITE_2), or null to auto-select
     */
    fun makeActivePlayerMove(playerId: String, rowColor: RowColor, whiteColor: DiceColor?): Boolean {
        if (!currentState.canMakeActivePlayerMove(playerId, this)) return false
        if (lockedRows.contains(rowColor)) return false
        if (whiteColor != null && whiteColor != DiceColor.WHITE_1 && whiteColor != DiceColor.WHITE_2) return false

        // Business logic
        val player = getPlayer(playerId) ?: return false
        val validMoves = calculateActivePlayerValidMoves()
        
        // Filter moves based on the specified white dice (if provided)
        val filteredMoves = if (whiteColor != null) {
            validMoves.filter { combination -> 
                // Must contain the row color
                combination.availableForRows.contains(rowColor) && 
                // Must contain the specified white dice
                combination.diceUsed.contains(whiteColor) &&
                // If WHITE_1 is specified, exclude combinations that use both white dice
                // If WHITE_2 is specified, exclude combinations that use both white dice
                !(combination.diceUsed.containsAll(listOf(DiceColor.WHITE_1, DiceColor.WHITE_2)))
            }
        } else {
            validMoves.filter { it.availableForRows.contains(rowColor) }
        }
        
        // If no valid moves with the specified white dice, return false
        if (filteredMoves.isEmpty()) return false
        
        // Select the move (first one if multiple are available)
        val move = filteredMoves.first()
        
        val newScoreSheet = player.scoreSheet.markNumber(rowColor, move.value) ?: return false

        updatePlayer(playerId, newScoreSheet)
        playersWhoMovedThisTurn.add(playerId)
        checkRowLocking(rowColor, newScoreSheet)
        checkGameEndConditions()

        // State transition
        currentState = currentState.onActivePlayerMove(this)
        return true
    }

    /**
     * Active player passes on mix move
     */
    fun passActivePlayerMove(playerId: String): Boolean {
        if (!currentState.canMakeActivePlayerMove(playerId, this)) return false

        // Active player passed - move to turn ended
        currentState = currentState.onActivePlayerMove(this)
        return true
    }

    /**
     * Manually transitions from white dice phase to active player phase
     */
    fun moveToActivePlayerPhase(): Boolean {
        if (!currentState.canMoveToActivePlayerPhase()) return false

        whiteDicePhaseCompleted = true  // Close white dice phase
        currentState = currentState.onMoveToActivePlayerPhase(this)
        return true
    }

    /**
     * Ends current turn and moves to next player
     */
    fun endTurn(): Boolean {
        if (!currentState.canEndTurn()) return false

        // Business logic - check if active player needs penalty
        val activePlayerId = getCurrentPlayer().id
        val activePlayerMoved = activePlayerId in playersWhoMovedThisTurn

        if (!activePlayerMoved) {
            addPenaltyToPlayer(activePlayerId)
        }

        // Move to the next player
        moveToNextPlayer()

        // State transition
        currentState = currentState.onEndTurn(this)
        return true
    }

    /**
     * Makes a move (either white dice or active player move)
     */
    fun makeMove(playerId: String, move: DiceCombination): Boolean {
        return if (move.isPass) {
            handlePassMove(playerId)
        } else {
            // Determine if this is white dice move or active player move
            when {
                !whiteDicePhaseCompleted && move.diceUsed.size == 2 &&
                        move.diceUsed.containsAll(listOf(DiceColor.WHITE_1, DiceColor.WHITE_2)) -> {
                    // White dice move
                    move.availableForRows.firstOrNull()?.let { rowColor ->
                        makeWhiteDiceMove(playerId, rowColor)
                    } ?: false
                }
                isCurrentPlayer(playerId) && move.diceUsed.size == 2 &&
                        (move.diceUsed.contains(DiceColor.WHITE_1) || move.diceUsed.contains(DiceColor.WHITE_2)) -> {
                    // Active player mix move
                    move.availableForRows.firstOrNull()?.let { rowColor ->
                        // Determine which white dice is being used
                        val whiteColor = when {
                            move.diceUsed.contains(DiceColor.WHITE_1) -> DiceColor.WHITE_1
                            move.diceUsed.contains(DiceColor.WHITE_2) -> DiceColor.WHITE_2
                            else -> null
                        }
                        makeActivePlayerMove(playerId, rowColor, whiteColor)
                    } ?: false
                }
                else -> false
            }
        }
    }

    /**
     * Gets all valid moves for a player based on the current state
     */
    fun getValidMoves(playerId: String): List<DiceCombination> {
        val normalMoves = currentState.getValidMoves(this, playerId)
        return normalMoves + DiceCombination.passMove()  // zawsze dostępny pas
    }

    /**
     * Gets valid moves for white dice phase
     */
    fun calculateWhiteDiceValidMoves(playerId: String): List<DiceCombination> {
        if (diceValues == null || whiteDicePhaseCompleted) return emptyList()

        val player = getPlayer(playerId) ?: return emptyList()
        val whiteDiceMoves = DiceRoller.calculateNonActivePlayerMoves(diceValues!!)

        return whiteDiceMoves.map { combination ->
            val availableRows = combination.availableForRows.filter { rowColor ->
                !lockedRows.contains(rowColor) &&
                        player.scoreSheet.isValidMove(rowColor, combination.value)
            }
            combination.copy(availableForRows = availableRows)
        }.filter { it.availableForRows.isNotEmpty() }
    }

    /**
     * Gets valid moves for active player phase
     */
    fun calculateActivePlayerValidMoves(): List<DiceCombination> {
        if (diceValues == null) return emptyList()

        val activePlayer = getCurrentPlayer()
        val activeMoves = DiceRoller.calculateActivePlayerMoves(diceValues!!)

        return activeMoves.map { combination ->
            val availableRows = combination.availableForRows.filter { rowColor ->
                !lockedRows.contains(rowColor) &&
                        activePlayer.scoreSheet.isValidMove(rowColor, combination.value)
            }
            combination.copy(availableForRows = availableRows)
        }.filter { it.availableForRows.isNotEmpty() }
    }

    /**
     * Checks if game is over
     */
    fun isGameOver(): Boolean = gameLifecycle == GameLifecycle.FINISHED

    /**
     * Gets winner (player with highest score)
     */
    fun getWinner(): Player? {
        return if (isGameOver()) {
            players.maxByOrNull { it.scoreSheet.calculateScore() }
        } else {
            null
        }
    }

    /**
     * Gets current active player
     */
    fun getCurrentPlayer(): Player = players[currentPlayerIndex]

    /**
     * Checks if player is the current active player
     */
    fun isCurrentPlayer(playerId: String): Boolean = getCurrentPlayer().id == playerId

    /**
     * Gets player by ID
     */
    fun getPlayer(playerId: String): Player? = players.find { it.id == playerId }

    /**
     * Updates player connection status
     */
    fun updatePlayerConnection(playerId: String, connected: Boolean): Boolean {
        val playerIndex = players.indexOfFirst { it.id == playerId }
        if (playerIndex == -1) return false

        players[playerIndex] = players[playerIndex].copy(isConnected = connected)
        return true
    }

    /**
     * Gets current turn state for external visibility
     */
    fun getCurrentTurnState(): TurnState = currentState.getTurnState()

    // Private helper methods (business logic)

    private fun handlePassMove(playerId: String): Boolean {
        return when {
            !whiteDicePhaseCompleted && currentState.canMakeWhiteDiceMove(playerId) -> {
                passWhiteDiceMove(playerId)
            }
            currentState.canMakeActivePlayerMove(playerId, this) -> {
                passActivePlayerMove(playerId)
            }
            else -> false
        }
    }

    private fun checkIfWhiteDicePhaseComplete() {
        // Check if all players have either moved or passed in white dice phase
        val allDecided = players.all { player ->
            player.id in playersWhoMovedThisTurn ||
                    player.id in playersWhoPassedWhiteDice
        }

        if (allDecided) {
            moveToActivePlayerPhase()
        }
    }

    private fun updatePlayer(playerId: String, newScoreSheet: ScoreSheet) {
        val playerIndex = players.indexOfFirst { it.id == playerId }
        val player = players[playerIndex]
        players[playerIndex] = player.copy(scoreSheet = newScoreSheet)
    }

    private fun checkRowLocking(rowColor: RowColor, scoreSheet: ScoreSheet) {
        if (scoreSheet.canLockRow(rowColor)) {
            lockedRows.add(rowColor)
        }
    }

    private fun addPenaltyToPlayer(playerId: String) {
        val player = getPlayer(playerId) ?: return
        val newScoreSheet = player.scoreSheet.addPenalty()
        updatePlayer(playerId, newScoreSheet)
        checkGameEndConditions()
    }

    private fun moveToNextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size
        playersWhoMovedThisTurn.clear()
        playersWhoPassedWhiteDice.clear()
        whiteDicePhaseCompleted = false
        diceValues = null
    }

    private fun checkGameEndConditions() {
        val shouldEnd = players.any { it.scoreSheet.shouldEndGame() } || lockedRows.size >= 2

        if (shouldEnd && gameLifecycle == GameLifecycle.IN_PROGRESS) {
            gameLifecycle = GameLifecycle.FINISHED
            finishedAt = LocalDateTime.now()
        }
    }
}

/**
 * Game lifecycle states
 */
enum class GameLifecycle {
    WAITING_FOR_PLAYERS,
    IN_PROGRESS,
    FINISHED
}

/**
 * Turn phase states (for external visibility)
 */
enum class TurnState {
    WAITING_FOR_DICE_ROLL,
    WHITE_DICE_PHASE,
    ACTIVE_PLAYER_PHASE,
    TURN_ENDED
}