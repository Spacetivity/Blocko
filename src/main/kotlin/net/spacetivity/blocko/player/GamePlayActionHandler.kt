package net.spacetivity.blocko.player

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.arena.Arena
import net.spacetivity.blocko.bossbar.BossbarHandler
import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.phase.GamePhaseMode
import net.spacetivity.blocko.phase.impl.IngamePhase
import net.spacetivity.blocko.translation.translateActionBar
import net.spacetivity.blocko.translation.translateMessage
import net.spacetivity.blocko.utils.Constants
import net.spacetivity.blocko.utils.PersistentDataUtils
import net.spacetivity.blocko.utils.formatTeamName
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask

class GamePlayActionHandler {

    private var mainTask: BukkitTask? = null
    private var movementTask: BukkitTask? = null
    private var playerTask: BukkitTask? = null

    fun startMainTask() {
        this.mainTask = Bukkit.getScheduler().runTaskTimerAsynchronously(Blocko.instance, Runnable {
            for (arena: Arena in Blocko.instance.arenaHandler.activeIngameArenas) {
                for (gamePlayer: GamePlayer in arena.currentPlayers) {
                    val player: Player = gamePlayer.toBukkitInstance() ?: continue

                    if (player.location.y <= (arena.yLevel - Constants.Y_LEVEL_FALL_THRESHOLD)) {
                        val yLevelDifference = arena.yLevel - player.location.y
                        player.teleportAsync(player.location.clone().add(0.0, yLevelDifference + 2.0, 1.0)).thenAccept {
                            player.isFlying = true
                        }
                    }

                    if (gamePlayer.getTeam().deactivated) continue

                    val ingamePhase: IngamePhase = arena.phase as IngamePhase
                    if (!ingamePhase.isInControllingTeam(gamePlayer.uuid) || ingamePhase.phaseMode != GamePhaseMode.PICK_ENTITY) continue

                    val currentItemStack = player.inventory.itemInMainHand

                    if (currentItemStack.type != Material.ARMOR_STAND) {
                        getHighlightedEntities(gamePlayer, arena).forEach { it.toggleHighlighting(false) }
                        continue
                    }

                    if (!PersistentDataUtils.has(currentItemStack.itemMeta, Constants.ENTITY_SELECTOR_KEY)) continue

                    val entityId: Int = PersistentDataUtils.get(
                        currentItemStack.itemMeta,
                        Constants.ENTITY_SELECTOR_KEY,
                        Int::class.java
                    )
                    val gameEntity: GameEntity =
                        Blocko.instance.gameEntityHandler.getEntitiesFromTeam(arena.id, gamePlayer.teamName!!)
                            .find { it.entityId == entityId }
                            ?: continue

                    getOtherHighlightedEntities(gamePlayer, arena, gameEntity).forEach { it.toggleHighlighting(false) }
                    gameEntity.toggleHighlighting(true)
                }
            }
        }, 0L, Constants.MAIN_TASK_TICK_INTERVAL)
    }

    fun startMovementTask() {
        this.movementTask = Bukkit.getScheduler().runTaskTimer(Blocko.instance, Runnable {
            for (gameEntity: GameEntity in Blocko.instance.gameEntityHandler.gameEntities.values()) {
                val arena = Blocko.instance.arenaHandler.getArena(gameEntity.arenaId) ?: continue
                if (!arena.phase.isIngame()) continue

                val ingamePhase: IngamePhase = arena.phase as IngamePhase

                if (!gameEntity.shouldMove) continue

                val gamePlayer: GamePlayer = gameEntity.controller ?: continue
                
                val gameTeam = gamePlayer.getTeam()
                if (gameTeam.deactivated) continue

                val dicedNumber: Int = gamePlayer.dicedNumber ?: continue

                val ignoreDicedNumber: Boolean = gamePlayer.lastEntityPickRule != null && gamePlayer.lastEntityPickRule == EntityPickRule.MOVABLE_OUT_OF_START
                val hasReachedGoal: Boolean = gameEntity.moveOneFieldForward(if (ignoreDicedNumber) 1 else dicedNumber)
                                
                if (!hasReachedGoal) continue

                gameEntity.lastStartField = null
                gameEntity.toggleHighlighting(false)

                if (gamePlayer.hasSavedAllEntities() && !arena.isGameOver() && !gameTeam.deactivated) {
                    gameTeam.deactivated = true

                    val position: Int = ingamePhase.getAmountOfFinishedTeams()

                    gamePlayer.matchStats.position = position

                    arena.sendArenaMessage(
                        "blocko.main_game_loop.player_finished_match",
                        Placeholder.parsed("team_color", "<${gameTeam.color.asHexString()}>"),
                        Placeholder.parsed("team_name", gameTeam.name.formatTeamName()),
                        Placeholder.parsed("position", position.toString())
                    )
                }

                if (arena.isGameOver()) {
                    Blocko.instance.gamePhaseHandler.nextPhase(arena)
                    continue
                }

                gameEntity.controller = null
                gameEntity.shouldMove = false

                gamePlayer.activeEntity = null
                gamePlayer.lastEntityPickRule = null
                gamePlayer.actionTimeoutTimestamp = null

                val newControllingTeam = ingamePhase.setNextControllingTeam() ?: continue

                gamePlayer.dicedNumber = null

                if (ingamePhase.lastControllingTeamId == ingamePhase.controllingTeamId) {
                    arena.sendArenaMessage(
                        "blocko.main_game_loop.can_dice_again",
                        Placeholder.parsed("team_color", "<${newControllingTeam.color.asHexString()}>"),
                        Placeholder.parsed("team_name", newControllingTeam.name.formatTeamName())
                    )
                } else {
                    arena.sendArenaMessage(
                        "blocko.main_game_loop.can_dice",
                        Placeholder.parsed("team_color", "<${newControllingTeam.color.asHexString()}>"),
                        Placeholder.parsed("team_name", newControllingTeam.name.formatTeamName())
                    )
                }

                ingamePhase.phaseMode = GamePhaseMode.DICE
            }
        }, 0L, Constants.MOVEMENT_TASK_TICK_INTERVAL)
    }

    fun startPlayerTask() {
        this.playerTask = Bukkit.getScheduler().runTaskTimer(Blocko.instance, Runnable {
            for (arena in Blocko.instance.arenaHandler.activeIngameArenas) {
                val ingamePhase: IngamePhase = arena.phase as IngamePhase
                val controllingGamePlayer: GamePlayer = ingamePhase.getControllingGamePlayer() ?: continue

                for (player: Player in arena.getAllPlayers()) {
                    if (controllingGamePlayer.actionTimeoutTimestamp != null) {
                        val bossbarHandler: BossbarHandler = Blocko.instance.bossbarHandler

                        val controllingGamePlayerTeam = Blocko.instance.gameTeamHandler.getTeamOfPlayer(
                            controllingGamePlayer.arenaId,
                            controllingGamePlayer.uuid
                        )
                            ?: continue

                        val timeLeft: Long = ingamePhase.getControllingGamePlayerTimeLeft()

                        val timePlaceholder =
                            Placeholder.parsed("time", if (timeLeft == 1L) "one" else timeLeft.toString())
                        val unitPlaceholder = Placeholder.parsed("unit", if (timeLeft == 1L) "second" else "seconds")

                        val timeColor: String =
                            if (timeLeft >= Constants.BOSSBAR_GREEN_THRESHOLD) NamedTextColor.GREEN.asHexString() else if (timeLeft >= Constants.BOSSBAR_YELLOW_THRESHOLD) NamedTextColor.YELLOW.asHexString() else NamedTextColor.DARK_RED.asHexString()
                        val timeColorPlaceholder = Placeholder.parsed("time_color", "<$timeColor>")

                        val bossbarText = Blocko.instance.translationHandler.getSelectedTranslation().line(
                            "blocko.bossbar.timeout",
                            Placeholder.parsed("team_color", "<${controllingGamePlayerTeam.color.asHexString()}>"),
                            Placeholder.parsed("team_name", controllingGamePlayerTeam.name.formatTeamName()),
                            timeColorPlaceholder,
                            timePlaceholder,
                            unitPlaceholder
                        )

                        if (bossbarHandler.getBossbars(player.uniqueId)
                                .none { it.first == Constants.TIMEOUT_BOSSBAR_NAME }
                        ) {
                            bossbarHandler.registerBossbar(
                                player,
                                Constants.TIMEOUT_BOSSBAR_NAME,
                                bossbarText,
                                1.0F,
                                BossBar.Color.GREEN,
                                BossBar.Overlay.PROGRESS
                            )
                        } else {
                            val progress: Float = ingamePhase.getControllingGamePlayerTimeLeftFraction()
                            bossbarHandler.updateBossbar(
                                player.uniqueId,
                                Constants.TIMEOUT_BOSSBAR_NAME,
                                BossbarHandler.BossBarUpdate.PROGRESS,
                                progress
                            )
                            bossbarHandler.updateBossbar(
                                player.uniqueId,
                                Constants.TIMEOUT_BOSSBAR_NAME,
                                BossbarHandler.BossBarUpdate.NAME,
                                bossbarText
                            )

                            val barColor: BossBar.Color =
                                if (timeLeft >= Constants.BOSSBAR_GREEN_THRESHOLD) BossBar.Color.GREEN else if (timeLeft >= Constants.BOSSBAR_YELLOW_THRESHOLD) BossBar.Color.YELLOW else BossBar.Color.RED
                            bossbarHandler.updateBossbar(
                                player.uniqueId,
                                Constants.TIMEOUT_BOSSBAR_NAME,
                                BossbarHandler.BossBarUpdate.COLOR,
                                barColor
                            )
                        }
                    }
                }

                for (gamePlayer: GamePlayer in arena.currentPlayers) {
                    if (gamePlayer.getTeam().deactivated) continue
                    if (!ingamePhase.isInControllingTeam(gamePlayer.uuid)) continue

                    if (!gamePlayer.isAI && gamePlayer.actionTimeoutTimestamp != null && (ingamePhase.phaseMode == GamePhaseMode.DICE || ingamePhase.phaseMode == GamePhaseMode.PICK_ENTITY)) {
                        if (System.currentTimeMillis() >= gamePlayer.actionTimeoutTimestamp!!) {
                            gamePlayer.activeEntity = null
                            gamePlayer.lastEntityPickRule = null
                            gamePlayer.dicedNumber = null
                            gamePlayer.actionTimeoutTimestamp = null

                            if (gamePlayer.isDicing()) Blocko.instance.diceHandler.dicingPlayers.remove(gamePlayer.uuid)

                            ingamePhase.phaseMode = GamePhaseMode.DICE
                            ingamePhase.setNextControllingTeam()

                            gamePlayer.translateMessage("blocko.main_game_loop.turn_expired")
                            gamePlayer.playSound(Sound.BLOCK_SCULK_SHRIEKER_HIT)
                            continue
                        }
                    }

                    when (ingamePhase.phaseMode) {
                        GamePhaseMode.DICE -> {
                            if (gamePlayer.isAI) gamePlayer.dice(ingamePhase)
                        }

                        GamePhaseMode.PICK_ENTITY -> {
                            if (gamePlayer.isAI) {
                                gamePlayer.autoPickEntity(ingamePhase)
                            } else {
                                val dicedNumber: Int = gamePlayer.dicedNumber ?: continue
                                val entitiesFromTeam: List<GameEntity> =
                                    Blocko.instance.gameEntityHandler.getEntitiesFromTeam(
                                        arena.id,
                                        gamePlayer.teamName!!
                                    )

                                if (entitiesFromTeam.all {
                                        (dicedNumber != 6 && it.currentFieldId == null) || !it.isMovableTo(
                                            gamePlayer.dicedNumber!!
                                        )
                                    }) {
                                    gamePlayer.activeEntity = null
                                    gamePlayer.lastEntityPickRule = null
                                    gamePlayer.actionTimeoutTimestamp = null

                                    Blocko.instance.bossbarHandler.unregisterBossbar(
                                        gamePlayer.toBukkitInstance()!!,
                                        Constants.TIMEOUT_BOSSBAR_NAME
                                    )

                                    ingamePhase.phaseMode = GamePhaseMode.DICE
                                    ingamePhase.setNextControllingTeam()
                                } else {
                                    gamePlayer.translateActionBar("blocko.main_game_loop.select_entity_notify")
                                }
                            }
                        }

                        GamePhaseMode.MOVE_ENTITY -> {
                            // Only call movePickedEntity() for non-AI players
                            // AI players already have movement initialized in autoPickEntity()
                            if (!gamePlayer.isAI) {
                                gamePlayer.movePickedEntity()
                            }
                        }
                    }

                }
            }
        }, 0L, Constants.PLAYER_TASK_TICK_INTERVAL)
    }

    fun stopTasks() {
        this.mainTask?.cancel()
        this.mainTask = null

        this.playerTask?.cancel()
        this.playerTask = null

        this.movementTask?.cancel()
        this.movementTask = null
    }

    private fun getHighlightedEntities(gamePlayer: GamePlayer, arena: Arena): List<GameEntity> {
        return Blocko.instance.gameEntityHandler.getEntitiesFromTeam(arena.id, gamePlayer.teamName!!)
            .filter { it.isHighlighted }
    }

    private fun getOtherHighlightedEntities(
        gamePlayer: GamePlayer,
        arena: Arena,
        highlightedEntity: GameEntity
    ): List<GameEntity> {
        return Blocko.instance.gameEntityHandler.getEntitiesFromTeam(arena.id, gamePlayer.teamName!!)
            .filter { it.livingEntity?.uniqueId != highlightedEntity.livingEntity?.uniqueId }
    }

}