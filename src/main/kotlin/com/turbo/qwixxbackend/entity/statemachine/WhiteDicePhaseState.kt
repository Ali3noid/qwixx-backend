package com.turbo.qwixxbackend.entity.statemachine

import com.turbo.qwixxbackend.entity.Game
import com.turbo.qwixxbackend.entity.TurnState
import com.turbo.qwixxbackend.util.DiceCombination

/**
 * White dice phase - all players can use white dice
 */
class WhiteDicePhaseState : GameState() {
    override fun canRollDice(): Boolean = false
    override fun canMakeWhiteDiceMove(playerId: String): Boolean = true
    override fun canMakeActivePlayerMove(playerId: String, game: Game): Boolean = false
    override fun canMoveToActivePlayerPhase(): Boolean = true
    override fun canEndTurn(): Boolean = false

    override fun onRollDice(game: Game): GameState = this
    override fun onWhiteDiceMove(game: Game): GameState = this // Stay in same state
    override fun onActivePlayerMove(game: Game): GameState = this
    override fun onMoveToActivePlayerPhase(game: Game): GameState = ActivePlayerPhaseState()
    override fun onEndTurn(game: Game): GameState = this

    override fun getValidMoves(game: Game, playerId: String): List<DiceCombination> {
        return game.calculateWhiteDiceValidMoves(playerId)
    }
    override fun getTurnState(): TurnState = TurnState.WHITE_DICE_PHASE
}