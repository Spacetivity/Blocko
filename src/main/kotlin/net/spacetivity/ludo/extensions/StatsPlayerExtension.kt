package net.spacetivity.ludo.extensions

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.stats.StatsPlayer
import net.spacetivity.ludo.stats.UpdateOperator
import net.spacetivity.ludo.stats.UpdateType
import org.bukkit.entity.Player

fun Player.addCoins(amount: Int) {
    val statsPlayer: StatsPlayer = LudoGame.instance.statsPlayerHandler.getCachedStatsPlayer(this.uniqueId) ?: return
    statsPlayer.update(UpdateType.COINS, UpdateOperator.INCREASE, amount)
}

fun Player.removeCoins(amount: Int) {
    val statsPlayer: StatsPlayer = LudoGame.instance.statsPlayerHandler.getCachedStatsPlayer(this.uniqueId) ?: return
    statsPlayer.update(UpdateType.COINS, UpdateOperator.DECREASE, amount)
}