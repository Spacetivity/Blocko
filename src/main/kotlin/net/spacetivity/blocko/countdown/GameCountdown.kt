package net.spacetivity.blocko.countdown

import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.arena.id.ArenaId
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask
import java.util.function.Predicate

abstract class GameCountdown(protected val arenaId: ArenaId, private val duration: Int) {

    var modifiableDuration = this.duration
    private var fallbackDuration = this.duration

    private var countdownTask: BukkitTask? = null
    var isRunning = false

    fun tryStartup(vararg startCondition: Predicate<Int>) {
        val gameArena = Blocko.instance.arenaHandler.getArena(this.arenaId) ?: return
        if (this.countdownTask != null) return
        if (startCondition.isNotEmpty() && !startCondition[0].test(gameArena.currentPlayers.size)) return

        isRunning = true
        this.countdownTask = Bukkit.getScheduler().runTaskTimer(Blocko.instance, Runnable {
            val remainingSeconds = this.modifiableDuration

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
        if (!this.isRunning) return
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