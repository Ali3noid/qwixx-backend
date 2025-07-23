package com.turbo.qwixxbackend.entity.statemachine

import com.turbo.qwixxbackend.entity.Game
import com.turbo.qwixxbackend.entity.TurnState
import com.turbo.qwixxbackend.util.DiceCombination

/**
 * Abstract base class for game states
 */
abstract class GameState {
    abstract fun canRollDice(): Boolean
    abstract fun canMakeWhiteDiceMove(playerId: String): Boolean
    abstract fun canMakeActivePlayerMove(playerId: String, game: Game): Boolean
    abstract fun canMoveToActivePlayerPhase(): Boolean
    abstract fun canEndTurn(): Boolean

    abstract fun onRollDice(game: Game): GameState
    abstract fun onWhiteDiceMove(game: Game): GameState
    abstract fun onActivePlayerMove(game: Game): GameState
    abstract fun onMoveToActivePlayerPhase(game: Game): GameState
    abstract fun onEndTurn(game: Game): GameState

    abstract fun getValidMoves(game: Game, playerId: String): List<DiceCombination>
    abstract fun getTurnState(): TurnState
}