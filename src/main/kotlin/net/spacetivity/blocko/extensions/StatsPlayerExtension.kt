package net.spacetivity.blocko.extensions

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.stats.StatsPlayer
import net.spacetivity.blocko.stats.UpdateOperation
import net.spacetivity.blocko.stats.StatsType
import org.bukkit.Sound
import org.bukkit.entity.Player

fun Player.addCoins(amount: Int, elimination: Boolean) {
    val statsPlayer: StatsPlayer = BlockoGame.instance.statsPlayerHandler.getStatsPlayer(this.uniqueId) ?: return
    statsPlayer.update(StatsType.COINS, UpdateOperation.INCREASE, amount)
    translateMessage(if (elimination) "blocko.coins.receive.on_kill" else "blocko.coins.receive.normal", Placeholder.parsed("amount", amount.toString()))
    playSound(this.location, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0F, 1.0F)
}

fun Player.removeCoins(amount: Int) {
    val statsPlayer: StatsPlayer = BlockoGame.instance.statsPlayerHandler.getStatsPlayer(this.uniqueId) ?: return
    statsPlayer.update(StatsType.COINS, UpdateOperation.DECREASE, amount)
}

fun GamePlayer.toStatsPlayerInstance(): StatsPlayer? {
    return BlockoGame.instance.statsPlayerHandler.getStatsPlayer(this.uuid)
}