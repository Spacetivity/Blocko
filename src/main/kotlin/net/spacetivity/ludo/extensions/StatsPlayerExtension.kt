package net.spacetivity.ludo.extensions

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.stats.StatsPlayer
import net.spacetivity.ludo.stats.UpdateOperator
import net.spacetivity.ludo.stats.UpdateType
import org.bukkit.Sound
import org.bukkit.entity.Player

fun Player.addCoins(amount: Int, elimination: Boolean) {
    val statsPlayer: StatsPlayer = LudoGame.instance.statsPlayerHandler.getStatsPlayer(this.uniqueId) ?: return
    statsPlayer.update(UpdateType.COINS, UpdateOperator.INCREASE, amount)
    translateMessage(if (elimination) "blocko.coins.receive.on_kill" else "blocko.coins.receive", Placeholder.parsed("amount", amount.toString()))
    playSound(this.location, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0F, 1.0F)
}

fun Player.removeCoins(amount: Int) {
    val statsPlayer: StatsPlayer = LudoGame.instance.statsPlayerHandler.getStatsPlayer(this.uniqueId) ?: return
    statsPlayer.update(UpdateType.COINS, UpdateOperator.DECREASE, amount)
}