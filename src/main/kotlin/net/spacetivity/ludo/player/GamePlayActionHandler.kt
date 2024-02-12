package net.spacetivity.ludo.player

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.entity.GameEntity
import net.spacetivity.ludo.extensions.sendMessage
import net.spacetivity.ludo.phase.GamePhaseMode
import net.spacetivity.ludo.phase.impl.IngamePhase
import net.spacetivity.ludo.team.GameTeam
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask

class GamePlayActionHandler {

    private var movementTask: BukkitTask? = null
    private var playerTask: BukkitTask? = null

    fun startMovementTask() {
        this.movementTask = Bukkit.getScheduler().runTaskTimer(LudoGame.instance, Runnable {
            for (gameEntity: GameEntity in LudoGame.instance.gameEntityHandler.gameEntities.values()) {
                if (!gameEntity.shouldMove) continue

                val controller: GamePlayer = gameEntity.controller ?: continue
                val dicedNumber: Int = controller.dicedNumber ?: continue

                val hasReachedGoal: Boolean = gameEntity.moveOneFieldForward(dicedNumber, 0.0)
                if (!hasReachedGoal) continue

                println("reached goal!")

                if (controller.hasWon()) {
                    println("Team ${gameEntity.teamName} has won!")
                    //TODO: Send GameWinEvent
                }

                val gameArena: GameArena = LudoGame.instance.gameArenaHandler.getArena(gameEntity.arenaId) ?: continue
                val ingamePhase: IngamePhase = gameArena.phase as IngamePhase

                ingamePhase.phaseMode = GamePhaseMode.DICE
                println("phase mode is now: ${ingamePhase.phaseMode.name}")

                gameEntity.controller = null
                gameEntity.shouldMove = false

                val newControllingTeam: GameTeam = ingamePhase.setNextControllingTeam() ?: continue
                gameArena.sendArenaMessage(Component.text("${newControllingTeam.name} can now dice!"))

                val gamePlayer: GamePlayer? = gameArena.currentPlayers.find { it.activeEntity!!.livingEntity!!.uniqueId == gameEntity.livingEntity!!.uniqueId }

                if (gamePlayer == null) {
                    println("Entity controller not found...")
                    continue
                }

                gamePlayer.inAction = false
                gamePlayer.activeEntity = null
            }
        }, 0L, 20L)
    }

    fun startPlayerTask() {
        this.playerTask = Bukkit.getScheduler().runTaskTimerAsynchronously(LudoGame.instance, Runnable {
            for (gameArena: GameArena in LudoGame.instance.gameArenaHandler.cachedArenas) {
                for (gamePlayer: GamePlayer in gameArena.currentPlayers) {
                    if (!gameArena.phase.isIngame()) continue
                    if (gamePlayer.inAction) continue

                    val ingamePhase: IngamePhase = gameArena.phase as IngamePhase
                    if (!ingamePhase.isInControllingTeam(gamePlayer.uuid)) continue

                    when (ingamePhase.phaseMode) {
                        GamePhaseMode.DICE -> {
                            if (gamePlayer.isAI) {
                                gamePlayer.dice()
                            } else {
                                gamePlayer.sendMessage(Component.text("Please dice now.", NamedTextColor.LIGHT_PURPLE))
                                gamePlayer.inAction = true
                            }
                        }

                        GamePhaseMode.PICK_ENTITY -> {
                            if (gamePlayer.isAI) {
                                gamePlayer.autoPickEntity(ingamePhase)
                                ingamePhase.phaseMode = GamePhaseMode.MOVE_ENTITY
                            } else {
                                gamePlayer.sendMessage(Component.text("Please select a entity now.", NamedTextColor.LIGHT_PURPLE))
                            }
                        }

                        GamePhaseMode.MOVE_ENTITY -> {
                            gamePlayer.movePickedEntity()
                        }
                    }

                }
            }
        }, 0L, 20L)
    }

    fun stopTasks() {
        if (this.playerTask != null) {
            this.playerTask!!.cancel()
            this.playerTask = null
        }

        if (this.movementTask != null) {
            this.movementTask!!.cancel()
            this.movementTask = null
        }
    }

}