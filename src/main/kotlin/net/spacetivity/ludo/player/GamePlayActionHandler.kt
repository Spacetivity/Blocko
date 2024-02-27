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
import net.spacetivity.ludo.utils.PersistentDataUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask

class GamePlayActionHandler {

    private var mainTask: BukkitTask? = null
    private var movementTask: BukkitTask? = null
    private var playerTask: BukkitTask? = null

    fun startMainTask() {
        this.mainTask = Bukkit.getScheduler().runTaskTimerAsynchronously(LudoGame.instance, Runnable {
            for (gameArena: GameArena in LudoGame.instance.gameArenaHandler.cachedArenas) {
                for (gamePlayer: GamePlayer in gameArena.currentPlayers) {
                    val player: Player = gamePlayer.toBukkitInstance() ?: continue

                    if (!gameArena.phase.isIngame()) continue

                    val ingamePhase: IngamePhase = gameArena.phase as IngamePhase
                    if (!ingamePhase.isInControllingTeam(gamePlayer.uuid) || ingamePhase.phaseMode != GamePhaseMode.PICK_ENTITY) continue

                    val currentItemStack: ItemStack = player.inventory.itemInMainHand
                    if (currentItemStack.type != Material.ARMOR_STAND) continue

                    if (!PersistentDataUtils.hasData(currentItemStack.itemMeta, "entitySelector")) continue

                    val entityId: Int = PersistentDataUtils.getData(currentItemStack.itemMeta, "entitySelector", Int::class.java)
                    val gameEntity: GameEntity = LudoGame.instance.gameEntityHandler.getEntitiesFromTeam(gameArena.id, gamePlayer.teamName).find { it.entityId == entityId } ?: continue

                    getOtherHighlightedEntities(gamePlayer, gameArena, gameEntity).forEach { it.toggleHighlighting(false) }
                    gameEntity.toggleHighlighting(true)
                }
            }
        }, 0L, 20L)
    }

    fun startMovementTask() {
        this.movementTask = Bukkit.getScheduler().runTaskTimer(LudoGame.instance, Runnable {
            for (gameEntity: GameEntity in LudoGame.instance.gameEntityHandler.gameEntities.values()) {
                if (!gameEntity.shouldMove) continue

                val gamePlayer: GamePlayer = gameEntity.controller ?: continue
                val dicedNumber: Int = gamePlayer.dicedNumber ?: continue

                val ignoreDicedNumber: Boolean = gamePlayer.lastEntityPickRule != null && gamePlayer.lastEntityPickRule == EntityPickRule.MOVABLE_OUT_OF_START
                val hasReachedGoal: Boolean = gameEntity.moveOneFieldForward(if (ignoreDicedNumber) 1 else dicedNumber, 0.0)
                if (!hasReachedGoal) continue

                gameEntity.lastStartField = null
                gameEntity.toggleHighlighting(false)
                println("reached goal!")

                if (gamePlayer.hasWon()) {
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

                if (ingamePhase.lastControllingTeamId == ingamePhase.controllingTeamId) {
                    gameArena.sendArenaMessage(Component.text("${newControllingTeam.name} can now dice again!"))
                } else {
                    gameArena.sendArenaMessage(Component.text("${newControllingTeam.name} can now dice!"))
                }

                gamePlayer.activeEntity = null
                gamePlayer.lastEntityPickRule = null
                gamePlayer.dicedNumber = null
            }
        }, 0L, 10L)
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

                                val dicedNumber: Int = gamePlayer.dicedNumber ?: continue
                                val entitiesFromTeam: List<GameEntity> = LudoGame.instance.gameEntityHandler.getEntitiesFromTeam(gameArena.id, gamePlayer.teamName)

                                // Move to the next player is GamePlayer cannot move out an entity or move any other entity at the field
                                if (entitiesFromTeam.all { (dicedNumber != 6 && it.currentFieldId == null) || !it.isMovableTo(gamePlayer.dicedNumber!!) }) {
                                    gamePlayer.activeEntity = null
                                    gamePlayer.lastEntityPickRule = null
                                    ingamePhase.phaseMode = GamePhaseMode.DICE
                                    ingamePhase.setNextControllingTeam()
                                }

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
        if (this.mainTask != null) {
            this.mainTask!!.cancel()
            this.mainTask = null
        }

        if (this.playerTask != null) {
            this.playerTask!!.cancel()
            this.playerTask = null
        }

        if (this.movementTask != null) {
            this.movementTask!!.cancel()
            this.movementTask = null
        }
    }

    private fun getOtherHighlightedEntities(gamePlayer: GamePlayer, gameArena: GameArena, highlightedEntity: GameEntity): List<GameEntity> {
        return LudoGame.instance.gameEntityHandler.getEntitiesFromTeam(gameArena.id, gamePlayer.teamName).filter { it.livingEntity?.uniqueId != highlightedEntity.livingEntity?.uniqueId }
    }

}