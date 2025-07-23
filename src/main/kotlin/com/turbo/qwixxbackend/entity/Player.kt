package com.turbo.qwixxbackend.entity

data class Player(
    val id: String,
    val name: String,
    val scoreSheet: ScoreSheet = ScoreSheet(),
    val isHost: Boolean = false,
    val isConnected: Boolean = true,
    val isAi: Boolean = false
)