package com.turbo.qwixxbackend.entity.statemachine

import com.turbo.qwixxbackend.entity.Game
import com.turbo.qwixxbackend.entity.TurnState
import com.turbo.qwixxbackend.util.DiceCombination

/**
 * Waiting for dice roll state
 */
class WaitingForDiceRollState : GameState() {
    override fun canRollDice(): Boolean = true
    override fun canMakeWhiteDiceMove(playerId: String): Boolean = false

    override fun canMakeActivePlayerMove(playerId: String, game: Game): Boolean = false
    override fun canMoveToActivePlayerPhase(): Boolean = false
    override fun canEndTurn(): Boolean = false

    override fun onRollDice(game: Game): GameState = WhiteDicePhaseState()
    override fun onWhiteDiceMove(game: Game): GameState = this
    override fun onActivePlayerMove(game: Game): GameState = this
    override fun onMoveToActivePlayerPhase(game: Game): GameState = this
    override fun onEndTurn(game: Game): GameState = this

    override fun getValidMoves(game: Game, playerId: String): List<DiceCombination> = emptyList()
    override fun getTurnState(): TurnState = TurnState.WAITING_FOR_DICE_ROLL
}