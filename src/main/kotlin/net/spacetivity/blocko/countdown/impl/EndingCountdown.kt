package net.spacetivity.blocko.countdown.impl

import net.kyori.adventure.text.Component
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.countdown.GameCountdown
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.stats.StatsPlayer
import net.spacetivity.blocko.stats.UpdateOperation
import net.spacetivity.blocko.stats.StatsType
import org.bukkit.Sound
import org.bukkit.scheduler.BukkitTask

class EndingCountdown(arenaId: String) : GameCountdown(arenaId, 5) {

    override fun handleCountdownIdle(countdownTask: BukkitTask, remainingSeconds: Int) {
        val gameArena: GameArena = BlockoGame.instance.gameArenaHandler.getArena(this.arenaId) ?: return
        val isOne = remainingSeconds == 1

        gameArena.sendArenaMessage(Component.text("Game stops in ${if (isOne) "one" else remainingSeconds} ${if (isOne) "second" else "seconds"}."))
        gameArena.sendArenaSound(Sound.ENTITY_PLAYER_LEVELUP,0.2F)
    }

    override fun handleCountdownEnd() {
        val gameArena: GameArena = BlockoGame.instance.gameArenaHandler.getArena(this.arenaId) ?: return

        for (gamePlayer: GamePlayer in gameArena.currentPlayers) {
            val statsPlayer: StatsPlayer = BlockoGame.instance.statsPlayerHandler.getStatsPlayer(gamePlayer.uuid) ?: continue
            statsPlayer.update(StatsType.PLAYED_GAMES, UpdateOperation.INCREASE, 1)
        }

        gameArena.sendArenaMessage(Component.text("Game arena resets now... You are teleported to spawn!"))
        gameArena.reset(false)
    }

}