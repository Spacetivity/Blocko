package net.spacetivity.blocko.countdown

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.GameArena
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.function.Predicate

abstract class GameCountdown(protected val arenaId: String, private val duration: Int) {

    var modifiableDuration: Int = this.duration
    private var fallbackDuration: Int = this.duration

    private var countdownTask: BukkitTask? = null
    var isRunning: Boolean = false

    fun tryStartup(vararg startCondition: Predicate<Int>) {
        val gameArena: GameArena = BlockoGame.instance.gameArenaHandler.getArena(this.arenaId) ?: return
        if (this.countdownTask != null) return
        if (startCondition.isNotEmpty() && !startCondition[0].test(gameArena.currentPlayers.size)) return

        isRunning = true
        this.countdownTask = Bukkit.getScheduler().runTaskTimer(BlockoGame.instance, Runnable {
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

    protected fun getCountdownProgress(currentTimerIndex: Int): Double {
        val startIndex: Int = this.fallbackDuration
        val percentage: Int = (currentTimerIndex / startIndex) * 100
        val decimal = BigDecimal(percentage).setScale(2, RoundingMode.HALF_UP)
        return decimal.toDouble()
    }

    abstract fun handleCountdownIdle(countdownTask: BukkitTask, remainingSeconds: Int)
    abstract fun handleCountdownEnd()

}