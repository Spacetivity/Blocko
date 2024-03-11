package net.spacetivity.ludo.stats

data class GamePlayerMatchStats(var eliminations: Int = 0, var knockedOutByOpponent: Int = 0, var gainedCoins: Int = 0, var position: Int? = null)