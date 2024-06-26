package net.spacetivity.blocko.player

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.bossbar.BossbarHandler
import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.extensions.*
import net.spacetivity.blocko.phase.GamePhaseMode
import net.spacetivity.blocko.phase.impl.IngamePhase
import net.spacetivity.blocko.team.GameTeam
import net.spacetivity.blocko.utils.PersistentDataUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask

class GamePlayActionHandler {

    private var mainTask: BukkitTask? = null
    private var movementTask: BukkitTask? = null
    private var playerTask: BukkitTask? = null

    fun startMainTask() {
        this.mainTask = Bukkit.getScheduler().runTaskTimerAsynchronously(BlockoGame.instance, Runnable {
            for (gameArena: GameArena in BlockoGame.instance.gameArenaHandler.cachedArenas.filter { it.phase.isIngame() }) {
                for (gamePlayer: GamePlayer in gameArena.currentPlayers) {
                    if (gamePlayer.getTeam().deactivated) continue
                    val player: Player = gamePlayer.toBukkitInstance() ?: continue

                    val ingamePhase: IngamePhase = gameArena.phase as IngamePhase
                    if (!ingamePhase.isInControllingTeam(gamePlayer.uuid) || ingamePhase.phaseMode != GamePhaseMode.PICK_ENTITY) continue

                    val currentItemStack: ItemStack = player.inventory.itemInMainHand

                    if (currentItemStack.type != Material.ARMOR_STAND) {
                        getHighlightedEntities(gamePlayer, gameArena).forEach { it.toggleHighlighting(false) }
                        continue
                    }

                    if (!PersistentDataUtils.hasData(currentItemStack.itemMeta, "entitySelector")) continue

                    val entityId: Int = PersistentDataUtils.getData(currentItemStack.itemMeta, "entitySelector", Int::class.java)
                    val gameEntity: GameEntity = BlockoGame.instance.gameEntityHandler.getEntitiesFromTeam(gameArena.id, gamePlayer.teamName!!).find { it.entityId == entityId }
                        ?: continue

                    getOtherHighlightedEntities(gamePlayer, gameArena, gameEntity).forEach { it.toggleHighlighting(false) }
                    gameEntity.toggleHighlighting(true)
                }
            }
        }, 0L, 1L)
    }

    fun startMovementTask() {
        this.movementTask = Bukkit.getScheduler().runTaskTimer(BlockoGame.instance, Runnable {
            for (gameEntity: GameEntity in BlockoGame.instance.gameEntityHandler.gameEntities.values()) {
                val gameArena: GameArena = BlockoGame.instance.gameArenaHandler.getArena(gameEntity.arenaId) ?: continue
                if (!gameArena.phase.isIngame()) continue

                val ingamePhase: IngamePhase = gameArena.phase as IngamePhase

                if (!gameEntity.shouldMove) continue

                val gamePlayer: GamePlayer = gameEntity.controller ?: continue

                val gameTeam: GameTeam = gamePlayer.getTeam()
                if (gameTeam.deactivated) continue

                val dicedNumber: Int = gamePlayer.dicedNumber ?: continue

                val ignoreDicedNumber: Boolean = gamePlayer.lastEntityPickRule != null && gamePlayer.lastEntityPickRule == EntityPickRule.MOVABLE_OUT_OF_START
                val hasReachedGoal: Boolean = gameEntity.moveOneFieldForward(if (ignoreDicedNumber) 1 else dicedNumber)
                if (!hasReachedGoal) continue

                gameEntity.lastStartField = null
                gameEntity.toggleHighlighting(false)

                if (gamePlayer.hasSavedAllEntities() && !gameArena.isGameOver() && !gameTeam.deactivated) {
                    gameTeam.deactivated = true

                    val position: Int = ingamePhase.getAmountOfFinishedTeams()

                    gamePlayer.matchStats.position = position
                    gameArena.sendArenaMessage(Component.text("TEAM ${gameTeam.name.uppercase()} HAS FINISHED (#$position)!", NamedTextColor.YELLOW, TextDecoration.BOLD))
                }

                if (gameArena.isGameOver()) {
                    BlockoGame.instance.gamePhaseHandler.nextPhase(gameArena)
                    continue
                }

                gameEntity.controller = null
                gameEntity.shouldMove = false

                gamePlayer.activeEntity = null
                gamePlayer.lastEntityPickRule = null
                gamePlayer.actionTimeoutTimestamp = null

                val newControllingTeam: GameTeam = ingamePhase.setNextControllingTeam() ?: continue

                gamePlayer.dicedNumber = null

                if (ingamePhase.lastControllingTeamId == ingamePhase.controllingTeamId) {
                    gameArena.sendArenaMessage(Component.text("${newControllingTeam.name} can now dice again!"))
                } else {
                    gameArena.sendArenaMessage(Component.text("${newControllingTeam.name} can now dice!"))
                }

                ingamePhase.phaseMode = GamePhaseMode.DICE
            }
        }, 0L, 10L)
    }

    fun startPlayerTask() {
        this.playerTask = Bukkit.getScheduler().runTaskTimerAsynchronously(BlockoGame.instance, Runnable {
            for (gameArena: GameArena in BlockoGame.instance.gameArenaHandler.cachedArenas.filter { it.phase.isIngame() }) {
                for (gamePlayer: GamePlayer in gameArena.currentPlayers) {

                    val ingamePhase: IngamePhase = gameArena.phase as IngamePhase

                    if (gamePlayer.getTeam().deactivated) continue
                    if (!ingamePhase.isInControllingTeam(gamePlayer.uuid)) continue

                    if (!gamePlayer.isAI && ingamePhase.isInControllingTeam(gamePlayer.uuid) && gamePlayer.actionTimeoutTimestamp != null) {
                        val bossbarHandler: BossbarHandler = BlockoGame.instance.bossbarHandler

                        val timeLeft: Long = ingamePhase.getControllingGamePlayerTimeLeft()

                        val timePlaceholder = Placeholder.parsed("time", if (timeLeft == 1L) "one" else timeLeft.toString())
                        val unitPlaceholder = Placeholder.parsed("unit", if (timeLeft == 1L) "second" else "seconds")

                        val timeColor: String = if (timeLeft >= 30) NamedTextColor.GREEN.asHexString() else if (timeLeft >= 10) NamedTextColor.YELLOW.asHexString() else NamedTextColor.DARK_RED.asHexString()
                        val timeColorPlaceholder = Placeholder.parsed("time_color", "<$timeColor>")

                        val bossbarText: Component = BlockoGame.instance.translationHandler.getSelectedTranslation().validateLine("blocko.bossbar.timeout", timeColorPlaceholder, timePlaceholder, unitPlaceholder)

                        if (bossbarHandler.getBossbars(gamePlayer.uuid).none { it.first == "timeoutBar" }) {
                            bossbarHandler.registerBossbar(gamePlayer.toBukkitInstance()!!, "timeoutBar", bossbarText, 1.0F, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS)
                        } else {
                            val progress: Float = ingamePhase.getControllingGamePlayerTimeLeftFraction()
                            bossbarHandler.updateBossbar(gamePlayer.uuid, "timeoutBar", BossbarHandler.BossBarUpdate.PROGRESS, progress)
                            bossbarHandler.updateBossbar(gamePlayer.uuid, "timeoutBar", BossbarHandler.BossBarUpdate.NAME, bossbarText)

                            val barColor: BossBar.Color = if (timeLeft >= 30) BossBar.Color.GREEN else if (timeLeft >= 10) BossBar.Color.YELLOW else BossBar.Color.RED
                            bossbarHandler.updateBossbar(gamePlayer.uuid, "timeoutBar", BossbarHandler.BossBarUpdate.COLOR, barColor)
                        }
                    }

                    if (!gamePlayer.isAI && gamePlayer.actionTimeoutTimestamp != null && (ingamePhase.phaseMode == GamePhaseMode.DICE || ingamePhase.phaseMode == GamePhaseMode.PICK_ENTITY)) {
                        if (System.currentTimeMillis() >= gamePlayer.actionTimeoutTimestamp!!) {
                            gamePlayer.activeEntity = null
                            gamePlayer.lastEntityPickRule = null
                            gamePlayer.dicedNumber = null
                            gamePlayer.actionTimeoutTimestamp = null
                            
                            if (gamePlayer.isDicing()) BlockoGame.instance.diceHandler.dicingPlayers.remove(gamePlayer.uuid)

                            ingamePhase.phaseMode = GamePhaseMode.DICE
                            ingamePhase.setNextControllingTeam()
                            gamePlayer.sendMessage(Component.text("You waited to long. Your turn is over!", NamedTextColor.DARK_RED))
                            gamePlayer.playSound(Sound.BLOCK_SCULK_SHRIEKER_HIT)
                            BlockoGame.instance.bossbarHandler.unregisterBossbar(gamePlayer.toBukkitInstance()!!, "timeoutBar")
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
                                val entitiesFromTeam: List<GameEntity> = BlockoGame.instance.gameEntityHandler.getEntitiesFromTeam(gameArena.id, gamePlayer.teamName!!)

                                if (entitiesFromTeam.all { (dicedNumber != 6 && it.currentFieldId == null) || !it.isMovableTo(gamePlayer.dicedNumber!!) }) {
                                    gamePlayer.activeEntity = null
                                    gamePlayer.lastEntityPickRule = null
                                    gamePlayer.dicedNumber = null
                                    gamePlayer.actionTimeoutTimestamp = null
                                    BlockoGame.instance.bossbarHandler.unregisterBossbar(gamePlayer.toBukkitInstance()!!, "timeoutBar")
                                    ingamePhase.phaseMode = GamePhaseMode.DICE
                                    ingamePhase.setNextControllingTeam()
                                } else {
                                    gamePlayer.sendActionBar(Component.text("Please select a entity now.", NamedTextColor.LIGHT_PURPLE))
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

    private fun getHighlightedEntities(gamePlayer: GamePlayer, gameArena: GameArena): List<GameEntity> {
        return BlockoGame.instance.gameEntityHandler.getEntitiesFromTeam(gameArena.id, gamePlayer.teamName!!).filter { it.isHighlighted }
    }

    private fun getOtherHighlightedEntities(gamePlayer: GamePlayer, gameArena: GameArena, highlightedEntity: GameEntity): List<GameEntity> {
        return BlockoGame.instance.gameEntityHandler.getEntitiesFromTeam(gameArena.id, gamePlayer.teamName!!).filter { it.livingEntity?.uniqueId != highlightedEntity.livingEntity?.uniqueId }
    }

}