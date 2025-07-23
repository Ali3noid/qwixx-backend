package com.turbo.qwixxbackend.entity.statemachine

import com.turbo.qwixxbackend.entity.Game
import com.turbo.qwixxbackend.entity.TurnState
import com.turbo.qwixxbackend.util.DiceCombination

/**
 * Active player phase - only active player can use white + colored dice
 */
class ActivePlayerPhaseState : GameState() {
    override fun canRollDice(): Boolean = false
    override fun canMakeWhiteDiceMove(playerId: String): Boolean = false
    override fun canMakeActivePlayerMove(playerId: String, game: Game): Boolean = game.isCurrentPlayer(playerId)
    override fun canMoveToActivePlayerPhase(): Boolean = false
    override fun canEndTurn(): Boolean = true

    override fun onRollDice(game: Game): GameState = this
    override fun onWhiteDiceMove(game: Game): GameState = this
    override fun onActivePlayerMove(game: Game): GameState = TurnEndedState()
    override fun onMoveToActivePlayerPhase(game: Game): GameState = this
    override fun onEndTurn(game: Game): GameState = WaitingForDiceRollState()

    override fun getValidMoves(game: Game, playerId: String): List<DiceCombination> {
        return if (game.isCurrentPlayer(playerId)) {
            game.calculateActivePlayerValidMoves()
        } else {
            emptyList()
        }
    }
    override fun getTurnState(): TurnState = TurnState.ACTIVE_PLAYER_PHASE
}