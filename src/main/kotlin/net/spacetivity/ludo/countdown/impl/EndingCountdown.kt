package net.spacetivity.ludo.countdown.impl

import net.kyori.adventure.text.Component
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.countdown.GameCountdown
import org.bukkit.Sound
import org.bukkit.scheduler.BukkitTask

class EndingCountdown(arenaId: String) : GameCountdown(arenaId, 5, null) {

    override fun handleCountdownIdle(countdownTask: BukkitTask, remainingSeconds: Int) {
        val gameArena: GameArena = LudoGame.instance.gameArenaHandler.getArena(this.arenaId) ?: return
        val isOne = remainingSeconds == 1

        gameArena.sendArenaMessage(Component.text("Game starts in ${if (isOne) "one" else remainingSeconds} ${if (isOne) "second" else "seconds"}."))
        gameArena.sendArenaSound(Sound.ENTITY_PLAYER_LEVELUP)
    }

    override fun handleCountdownEnd() {
        val gameArena: GameArena = LudoGame.instance.gameArenaHandler.getArena(this.arenaId) ?: return
        gameArena.sendArenaMessage(Component.text("Game arena resets now... You are teleported to spawn!"))
        gameArena.reset()
    }

}