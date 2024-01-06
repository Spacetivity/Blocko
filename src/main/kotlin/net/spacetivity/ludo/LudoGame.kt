package net.spacetivity.ludo

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.arena.GameArenaHandler
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.*

class LudoGame : JavaPlugin() {

    lateinit var gameArenaHandler: GameArenaHandler
    var idleTask: BukkitTask? = null

    override fun onEnable() {
        instance = this
        this.gameArenaHandler = GameArenaHandler()
    }

    override fun onDisable() {
        this.idleTask?.cancel()
        this.gameArenaHandler.resetArenas()
    }

    companion object {
        @JvmStatic
        lateinit var instance: LudoGame
            private set
    }

    fun tryStartupIdleScheduler() {
        if (!emptyArenasPreset().first) return

        this.idleTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, Runnable {
            val validArenas = emptyArenasPreset().second

            if (!emptyArenasPreset().first) {
                this.idleTask?.cancel()
                this.idleTask = null
            }

            for (gameArena: GameArena in validArenas) {

                //TODO: Check if arena is in 'lobby' state!

                for (uuid: UUID in gameArena.currentPlayers) {
                    val player: Player = Bukkit.getPlayer(uuid) ?: continue
                    player.sendActionBar(Component.text("Waiting for more players...", NamedTextColor.RED))
                }
            }
        }, 0L, 20L)
    }

    fun tryShutdownIdleScheduler(){
        if (this.idleTask == null) return
        if (emptyArenasPreset().first) return

        this.idleTask?.cancel()
        this.idleTask = null
    }

    private fun emptyArenasPreset(): Pair<Boolean, List<GameArena>> {
        val emptyArenas = this.gameArenaHandler.cachedArenas.filter { it.currentPlayers.size < it.maxPlayers }.toList()
        return Pair(emptyArenas.isNotEmpty(), emptyArenas)
    }

}