package net.spacetivity.ludo.player

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.bossbar.BossbarHandler
import net.spacetivity.ludo.entity.GameEntity
import net.spacetivity.ludo.extensions.sendActionBar
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

                gamePlayer.activeEntity = null
            }
        }, 0L, 20L)
    }

    fun startPlayerTask() {
        this.playerTask = Bukkit.getScheduler().runTaskTimerAsynchronously(LudoGame.instance, Runnable {
            for (gameArena: GameArena in LudoGame.instance.gameArenaHandler.cachedArenas) {
                for (gamePlayer: GamePlayer in gameArena.currentPlayers) {
                    if (!gameArena.phase.isIngame()) continue

                    val ingamePhase: IngamePhase = gameArena.phase as IngamePhase

                    val bossbarHandler: BossbarHandler = LudoGame.instance.bossbarHandler
                    val currentGamePlayerName: String? = if (ingamePhase.getControllingTeam()?.name == gamePlayer.teamName) "you" else ingamePhase.getControllingTeam()?.name
                    val bossbarText: TextComponent = Component.text("Current game player >> $currentGamePlayerName")

                    if (!gamePlayer.isAI) {
                        if (bossbarHandler.getBossbars(gamePlayer.uuid).none { it.first == "currentPlayerInfo" }) {
                            bossbarHandler.registerBossbar(gamePlayer.toBukkitInstance()!!, "currentPlayerInfo", bossbarText, 1.0F, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS)
                        } else {
                            bossbarHandler.updateBossbar(gamePlayer.uuid, "currentPlayerInfo", BossbarHandler.BossBarUpdate.NAME, bossbarText)
                        }
                    }

                    if (!gamePlayer.inAction()) continue

                    when (ingamePhase.phaseMode) {
                        GamePhaseMode.DICE -> {
                            if (gamePlayer.isAI) {
                                gamePlayer.dice()
                            } else {
                                gamePlayer.sendActionBar(Component.text("Please dice now.", NamedTextColor.LIGHT_PURPLE))
                            }
                        }

                        GamePhaseMode.PICK_ENTITY -> {
                            if (gamePlayer.isAI) {
                                gamePlayer.autoPickEntity(ingamePhase)
                            } else {
                                gamePlayer.sendActionBar(Component.text("Please select a entity now.", NamedTextColor.LIGHT_PURPLE))
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