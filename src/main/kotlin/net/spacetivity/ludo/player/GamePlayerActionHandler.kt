package net.spacetivity.ludo.player

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.phase.GamePhaseMode
import net.spacetivity.ludo.phase.impl.IngamePhase
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask

class GamePlayerActionHandler {

    private var bukkitTask: BukkitTask? = null

    fun startActionScheduler() {
        val gamePlayers: MutableList<GamePlayer> = mutableListOf() //TODO: ONLY DEMO | Change for actual arena game players!

        this.bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(LudoGame.instance, Runnable {
            for (gamePlayer: GamePlayer in gamePlayers) {
                val gameArena: GameArena = LudoGame.instance.gameArenaHandler.getArena(gamePlayer.arenaId) ?: continue

                if (!gameArena.phase.isIngame()) continue

                val ingamePhase: IngamePhase = gameArena.phase as IngamePhase
                if (!ingamePhase.isInControllingTeam(gamePlayer.uuid)) continue

                when (ingamePhase.phaseMode) {
                    GamePhaseMode.DICE -> gamePlayer.dice()
                    GamePhaseMode.PICK_ENTITY -> gamePlayer.pickEntity()
                    GamePhaseMode.MOVE_ENTITY -> gamePlayer.movePickedEntity(0.0) //TODO: Change this to actual field height
                }

            }
        }, 0L, 20L)
    }

    fun stopActionScheduler() {
        if (this.bukkitTask == null) return
        this.bukkitTask!!.cancel()
        this.bukkitTask = null
    }

}