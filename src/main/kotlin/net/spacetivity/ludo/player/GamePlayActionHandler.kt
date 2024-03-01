package net.spacetivity.ludo.player

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.bossbar.BossbarHandler
import net.spacetivity.ludo.entity.GameEntity
import net.spacetivity.ludo.extensions.getTeam
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
            for (gameArena: GameArena in LudoGame.instance.gameArenaHandler.cachedArenas.filter { it.phase.isIngame() }) {
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
                    val gameEntity: GameEntity = LudoGame.instance.gameEntityHandler.getEntitiesFromTeam(gameArena.id, gamePlayer.teamName).find { it.entityId == entityId }
                        ?: continue

                    getOtherHighlightedEntities(gamePlayer, gameArena, gameEntity).forEach { it.toggleHighlighting(false) }
                    gameEntity.toggleHighlighting(true)
                }
            }
        }, 0L, 1L)
    }

    fun startMovementTask() {
        this.movementTask = Bukkit.getScheduler().runTaskTimer(LudoGame.instance, Runnable {
            for (gameEntity: GameEntity in LudoGame.instance.gameEntityHandler.gameEntities.values()) {
                val gameArena: GameArena = LudoGame.instance.gameArenaHandler.getArena(gameEntity.arenaId) ?: continue
                if (gameArena.phase.isIngame()) //TODO: check if this causes bugs

                if (!gameEntity.shouldMove) continue

                val gamePlayer: GamePlayer = gameEntity.controller ?: continue
                val gameTeam: GameTeam = gamePlayer.getTeam()

                if (gameTeam.deactivated) continue

                val dicedNumber: Int = gamePlayer.dicedNumber ?: continue

                val ignoreDicedNumber: Boolean = gamePlayer.lastEntityPickRule != null && gamePlayer.lastEntityPickRule == EntityPickRule.MOVABLE_OUT_OF_START
                val hasReachedGoal: Boolean = gameEntity.moveOneFieldForward(if (ignoreDicedNumber) 1 else dicedNumber, 0.0)
                if (!hasReachedGoal) continue

                gameEntity.lastStartField = null
                gameEntity.toggleHighlighting(false)
                println("reached goal!")

                if (gamePlayer.hasSavedAllEntities() && !gameArena.isGameOver() && !gameTeam.deactivated) {
                    gameTeam.deactivated = true
                    gameArena.sendArenaMessage(Component.text("TEAM ${gameTeam.name.uppercase()} HAS FINISHED!", NamedTextColor.YELLOW, TextDecoration.BOLD))
                    println("Team ${gameEntity.teamName} has saved all entities!")
                }

                if (gameArena.isGameOver()) { //TODO: Vielleicht muss das woanders hin!!!!111!! LOL XD ROFL DU KEK AMK TAFNIES !!!"132h3h34fgbh2tgbh4TBh4tgbvh$TGB4htgb4hfgb4hefb4hB!H§F1bh3
                    LudoGame.instance.gamePhaseHandler.nextPhase(gameArena) //TODO: check if this causes bugs
                    continue
                }

                val ingamePhase: IngamePhase = gameArena.phase as IngamePhase

                gameEntity.controller = null
                gameEntity.shouldMove = false

                gamePlayer.activeEntity = null
                gamePlayer.lastEntityPickRule = null

                val newControllingTeam: GameTeam = ingamePhase.setNextControllingTeam() ?: continue

                gamePlayer.dicedNumber = null

                if (ingamePhase.lastControllingTeamId == ingamePhase.controllingTeamId) {
                    gameArena.sendArenaMessage(Component.text("${newControllingTeam.name} can now dice again!"))
                } else {
                    gameArena.sendArenaMessage(Component.text("${newControllingTeam.name} can now dice!"))
                }

                ingamePhase.phaseMode = GamePhaseMode.DICE
                println("phase mode is now: ${ingamePhase.phaseMode.name}")
            }
        }, 0L, 10L)
    }

    fun startPlayerTask() {
        this.playerTask = Bukkit.getScheduler().runTaskTimerAsynchronously(LudoGame.instance, Runnable {
            for (gameArena: GameArena in LudoGame.instance.gameArenaHandler.cachedArenas.filter { it.phase.isIngame() }) {
                for (gamePlayer: GamePlayer in gameArena.currentPlayers) {
                    if (gamePlayer.getTeam().deactivated) continue

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

                    if (!ingamePhase.isInControllingTeam(gamePlayer.uuid)) continue

                    when (ingamePhase.phaseMode) {
                        GamePhaseMode.DICE -> {
                            if (gamePlayer.isAI) {
                                gamePlayer.dice(ingamePhase)
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
                                    gamePlayer.dicedNumber = null
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

    private fun getHighlightedEntities(gamePlayer: GamePlayer, gameArena: GameArena): List<GameEntity> {
        return LudoGame.instance.gameEntityHandler.getEntitiesFromTeam(gameArena.id, gamePlayer.teamName).filter { it.isHighlighted }
    }

    private fun getOtherHighlightedEntities(gamePlayer: GamePlayer, gameArena: GameArena, highlightedEntity: GameEntity): List<GameEntity> {
        return LudoGame.instance.gameEntityHandler.getEntitiesFromTeam(gameArena.id, gamePlayer.teamName).filter { it.livingEntity?.uniqueId != highlightedEntity.livingEntity?.uniqueId }
    }

}