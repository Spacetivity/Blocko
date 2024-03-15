package net.spacetivity.ludo.countdown.impl

import net.kyori.adventure.text.Component
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.countdown.GameCountdown
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.stats.StatsPlayer
import net.spacetivity.ludo.stats.UpdateOperation
import net.spacetivity.ludo.stats.UpdateType
import org.bukkit.Sound
import org.bukkit.scheduler.BukkitTask

class EndingCountdown(arenaId: String) : GameCountdown(arenaId, 5) {

    override fun handleCountdownIdle(countdownTask: BukkitTask, remainingSeconds: Int) {
        val gameArena: GameArena = LudoGame.instance.gameArenaHandler.getArena(this.arenaId) ?: return
        val isOne = remainingSeconds == 1

        gameArena.sendArenaMessage(Component.text("Game stops in ${if (isOne) "one" else remainingSeconds} ${if (isOne) "second" else "seconds"}."))
        gameArena.sendArenaSound(Sound.ENTITY_PLAYER_LEVELUP,0.2F)
    }

    override fun handleCountdownEnd() {
        val gameArena: GameArena = LudoGame.instance.gameArenaHandler.getArena(this.arenaId) ?: return

        for (gamePlayer: GamePlayer in gameArena.currentPlayers) {
            val statsPlayer: StatsPlayer = LudoGame.instance.statsPlayerHandler.getStatsPlayer(gamePlayer.uuid) ?: continue
            statsPlayer.update(UpdateType.PLAYED_GAMES, UpdateOperation.INCREASE, 1)
        }

        gameArena.sendArenaMessage(Component.text("Game arena resets now... You are teleported to spawn!"))
        gameArena.reset(false)
    }

}