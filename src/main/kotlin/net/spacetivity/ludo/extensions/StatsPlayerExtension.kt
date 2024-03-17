package net.spacetivity.ludo.extensions

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.stats.StatsPlayer
import net.spacetivity.ludo.stats.UpdateOperation
import net.spacetivity.ludo.stats.StatsType
import org.bukkit.Sound
import org.bukkit.entity.Player

fun Player.addCoins(amount: Int, elimination: Boolean) {
    val statsPlayer: StatsPlayer = LudoGame.instance.statsPlayerHandler.getStatsPlayer(this.uniqueId) ?: return
    statsPlayer.update(StatsType.COINS, UpdateOperation.INCREASE, amount)
    translateMessage(if (elimination) "blocko.coins.receive.on_kill" else "blocko.coins.receive.normal", Placeholder.parsed("amount", amount.toString()))
    playSound(this.location, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0F, 1.0F)
}

fun Player.removeCoins(amount: Int) {
    val statsPlayer: StatsPlayer = LudoGame.instance.statsPlayerHandler.getStatsPlayer(this.uniqueId) ?: return
    statsPlayer.update(StatsType.COINS, UpdateOperation.DECREASE, amount)
}

fun GamePlayer.toStatsPlayerInstance(): StatsPlayer? {
    return LudoGame.instance.statsPlayerHandler.getStatsPlayer(this.uuid)
}