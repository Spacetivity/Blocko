package net.spacetivity.ludo.countdown.impl

import net.kyori.adventure.text.Component
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.countdown.GameCountdown
import org.bukkit.Sound
import org.bukkit.scheduler.BukkitTask
import java.util.function.Predicate

class IdleCountdown(arenaId: String) : GameCountdown(arenaId, 20, Predicate { t -> t >= 1 }) {

    override fun handleCountdownIdle(countdownTask: BukkitTask, remainingSeconds: Int) {
        if (remainingSeconds % 10 == 0 || remainingSeconds < 6) {
            val gameArena: GameArena = LudoGame.instance.gameArenaHandler.getArena(this.arenaId) ?: return
            val isOne = remainingSeconds == 1
            gameArena.sendArenaMessage(Component.text("Game starts in ${if (isOne) "one" else remainingSeconds} ${if (isOne) "second" else "seconds"}."))
            gameArena.sendArenaSound(Sound.ENTITY_PLAYER_LEVELUP)
        }
    }

    override fun handleCountdownEnd() {
        val gameArena: GameArena = LudoGame.instance.gameArenaHandler.getArena(this.arenaId) ?: return
        LudoGame.instance.gamePhaseHandler.nextPhase(gameArena)
    }

//    private fun getCountdownProgress(currentTimerIndex: Int): Double {
//        val startIndex: Int = this.fallbackDuration
//        val percentage: Int = (currentTimerIndex / startIndex) * 100
//        val decimal = BigDecimal(percentage).setScale(2, RoundingMode.HALF_UP)
//        return decimal.toDouble()
//    }

}