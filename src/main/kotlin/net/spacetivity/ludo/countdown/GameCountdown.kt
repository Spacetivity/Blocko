package net.spacetivity.ludo.countdown

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask
import java.util.function.Predicate

abstract class GameCountdown(protected val arenaId: String, private val duration: Int, private val startCondition: Predicate<Int>) {

    var fallbackDuration: Int = this.duration
    var modifiableDuration: Int = this.duration

    var countdownTask: BukkitTask? = null
    var isRunning: Boolean = false

    fun tryStartup() {
        val gameArena: GameArena = LudoGame.instance.gameArenaHandler.getArena(this.arenaId) ?: return
        if (this.countdownTask != null || !this.startCondition.test(gameArena.currentPlayers.size)) return

        isRunning = true
        this.countdownTask = Bukkit.getScheduler().runTaskTimerAsynchronously(LudoGame.instance, Runnable {
            val remainingSeconds: Int = this.modifiableDuration

            if (remainingSeconds == 0) {
                stop()
                return@Runnable
            }

            handleCountdownIdle(this.countdownTask!!, remainingSeconds)
            this.modifiableDuration = remainingSeconds.dec()

        }, 0L, 20L)
    }

    fun stop() {
        if (this.countdownTask == null) return
        this.isRunning = false
        this.countdownTask!!.cancel()
        this.countdownTask = null
        this.modifiableDuration = this.fallbackDuration
        handleCountdownEnd()
    }

    fun cancel() {
        if (this.countdownTask == null) return
        this.isRunning = false
        this.countdownTask!!.cancel()
        this.countdownTask = null
        this.modifiableDuration = this.fallbackDuration
    }

    abstract fun handleCountdownIdle(countdownTask: BukkitTask, remainingSeconds: Int)
    abstract fun handleCountdownEnd()

}