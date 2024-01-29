package net.spacetivity.ludo.player

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.entity.GameEntity
import net.spacetivity.ludo.extensions.sendMessage
import net.spacetivity.ludo.phase.GamePhaseMode
import net.spacetivity.ludo.phase.impl.IngamePhase
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask

class GamePlayActionTasks {

    private var movementTask: BukkitTask? = null
    private var playerTask: BukkitTask? = null

    fun startMovementTask() {
        this.movementTask = Bukkit.getScheduler().runTaskTimerAsynchronously(LudoGame.instance, Runnable {
            for (gameEntity: GameEntity in LudoGame.instance.gameEntityHandler.gameEntities.values()) {
                if (!gameEntity.shouldMove) continue

                val controller: GamePlayer = gameEntity.controller ?: continue
                val dicedNumber: Int = controller.dicedNumber ?: continue

                val hasReachedGoal: Boolean = gameEntity.move(dicedNumber, 0.0)
                if (!hasReachedGoal) continue

                if (controller.hasWon()) {

                }

                gameEntity.controller = null
                gameEntity.shouldMove = false
            }
        }, 0L, 20L)
    }

    fun startPlayerTask() {
        val gamePlayers: MutableList<GamePlayer> = mutableListOf() //TODO: ONLY DEMO | Change for actual arena game players!

        this.playerTask = Bukkit.getScheduler().runTaskTimerAsynchronously(LudoGame.instance, Runnable {
            for (gamePlayer: GamePlayer in gamePlayers) {
                val gameArena: GameArena = LudoGame.instance.gameArenaHandler.getArena(gamePlayer.arenaId) ?: continue

                if (!gameArena.phase.isIngame()) continue
                if (gamePlayer.inAction) continue

                val ingamePhase: IngamePhase = gameArena.phase as IngamePhase
                if (!ingamePhase.isInControllingTeam(gamePlayer.uuid)) continue

                when (ingamePhase.phaseMode) {
                    GamePhaseMode.DICE -> {
                        if (gamePlayer.isAI) {
                            gamePlayer.dice()
                        } else {
                            gamePlayer.sendMessage(Component.text("Please dice now. :)", NamedTextColor.LIGHT_PURPLE))
                            gamePlayer.inAction = true
                        }
                    }

                    GamePhaseMode.PICK_ENTITY -> {
                        if (gamePlayer.isAI) {
                            gamePlayer.autoPickEntity()
                            ingamePhase.phaseMode = GamePhaseMode.MOVE_ENTITY
                        }
                    }

                    GamePhaseMode.MOVE_ENTITY -> {
                        gamePlayer.movePickedEntity(0.0)
                        ingamePhase.phaseMode = GamePhaseMode.DICE
                    }
                }

            }
        }, 0L, 20L)
    }

    fun stopTasks() {
        if (this.playerTask == null) return
        this.playerTask!!.cancel()
        this.playerTask = null
    }

}