package net.spacetivity.blocko.player

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.bossbar.BossbarHandler
import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.phase.GamePhaseMode
import net.spacetivity.blocko.phase.impl.IngamePhase
import net.spacetivity.blocko.team.GameTeam
import net.spacetivity.blocko.translation.translateActionBar
import net.spacetivity.blocko.translation.translateMessage
import net.spacetivity.blocko.utils.Constants
import net.spacetivity.blocko.utils.PersistentDataUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.ThreadLocalRandom

class GamePlayActionHandler {

    private var mainTask: BukkitTask? = null
    private var movementTask: BukkitTask? = null
    private var playerTask: BukkitTask? = null

    fun startMainTask() {
        mainTask = Bukkit.getScheduler().runTaskTimerAsynchronously(BlockoGame.instance, Runnable {
            for (arena in BlockoGame.instance.gameArenaHandler.cachedArenas.filter { it.phase.isIngame() }) {
                handleArenaMainTask(arena)
            }
        }, 0L, 1L)
    }

    fun startMovementTask() {
        movementTask = Bukkit.getScheduler().runTaskTimer(BlockoGame.instance, Runnable {
            for (entity in BlockoGame.instance.gameEntityHandler.gameEntities.values()) {
                handleEntityMovement(entity)
            }
        }, 0L, 10L)
    }

    fun startPlayerTask() {
        playerTask = Bukkit.getScheduler().runTaskTimerAsynchronously(BlockoGame.instance, Runnable {
            for (arena in BlockoGame.instance.gameArenaHandler.cachedArenas.filter { it.phase.isIngame() }) {
                handleArenaPlayerTask(arena)
            }
        }, 0L, 10L)
    }

    fun stopTasks() {
        mainTask?.cancel().also { mainTask = null }
        movementTask?.cancel().also { movementTask = null }
        playerTask?.cancel().also { playerTask = null }
    }

    private fun handleArenaMainTask(arena: GameArena) {
        val ingamePhase = arena.phase as? IngamePhase ?: return
        arena.currentPlayers.forEach { gamePlayer ->
            val bukkitPlayer = gamePlayer.toBukkitInstance() ?: return@forEach

            handlePlayerTeleportIfNeeded(bukkitPlayer, arena)
            if (gamePlayer.getTeam().deactivated) return@forEach

            if (!ingamePhase.isInControllingTeam(gamePlayer.uuid) || ingamePhase.phaseMode != GamePhaseMode.PICK_ENTITY)
                return@forEach

            handleEntityHighlighting(gamePlayer, arena)
        }
    }

    private fun handlePlayerTeleportIfNeeded(player: Player, arena: GameArena) {
        if (player.location.y <= (arena.yLevel - 10)) {
            val yDiff = arena.yLevel - player.location.y
            player.teleportAsync(player.location.clone().add(0.0, yDiff + 2.0, 1.0)).thenAccept {
                player.isFlying = true
            }
        }
    }

    private fun handleEntityHighlighting(gamePlayer: GamePlayer, arena: GameArena) {
        val player = gamePlayer.toBukkitInstance() ?: return
        val itemStack: ItemStack = player.inventory.itemInMainHand

        if (itemStack.type != Material.ARMOR_STAND) {
            getHighlightedEntities(gamePlayer, arena).forEach { it.toggleHighlighting(false) }
            return
        }
        if (!PersistentDataUtils.hasData(itemStack.itemMeta, "entitySelector"))
            return

        val entityId: Int = PersistentDataUtils.getData(itemStack.itemMeta, "entitySelector", Int::class.java)
        val gameEntity = BlockoGame.instance.gameEntityHandler
            .getEntitiesFromTeam(arena.id, gamePlayer.teamName!!).find { it.entityId == entityId } ?: return

        getOtherHighlightedEntities(gamePlayer, arena, gameEntity).forEach { it.toggleHighlighting(false) }
        gameEntity.toggleHighlighting(true)
    }

    private fun handleEntityMovement(entity: GameEntity) {
        val arena = BlockoGame.instance.gameArenaHandler.getArena(entity.arenaId) ?: return
        if (!arena.phase.isIngame()) return
        val ingamePhase = arena.phase as? IngamePhase ?: return

        if (!entity.shouldMove) return

        val controller = entity.controller ?: return
        val team = controller.getTeam()
        if (team.deactivated) return

        val dicedNumber = controller.dicedNumber ?: return
        val ignoreDiced = controller.lastEntityPickRule == EntityPickRule.MOVABLE_OUT_OF_START

        if (!entity.moveOneFieldForward(if (ignoreDiced) 1 else dicedNumber)) return

        entity.lastStartField = null
        entity.toggleHighlighting(false)

        if (controller.hasSavedAllEntities() && !arena.isGameOver() && !team.deactivated) {
            team.deactivated = true
            val position = ingamePhase.getAmountOfFinishedTeams()
            controller.matchStats.position = position
            arena.sendArenaMessage(
                "blocko.main_game_loop.player_finished_match",
                Placeholder.parsed("team_color", "<${team.color.asHexString()}>"),
                Placeholder.parsed("team_name", team.name.lowercase().replaceFirstChar { it.uppercase() }),
                Placeholder.parsed("position", position.toString())
            )
        }

        if (arena.isGameOver()) {
            BlockoGame.instance.gamePhaseHandler.nextPhase(arena)
            return
        }

        entity.controller = null
        entity.shouldMove = false

        controller.activeEntity = null
        controller.lastEntityPickRule = null
        controller.actionTimeoutTimestamp = null
        controller.dicedNumber = null

        val newTeam = ingamePhase.setNextControllingTeam() ?: return
        val messageKey = if (ingamePhase.lastControllingTeamId == ingamePhase.controllingTeamId)
            "blocko.main_game_loop.can_dice_again"
        else
            "blocko.main_game_loop.can_dice"
        arena.sendArenaMessage(
            messageKey,
            Placeholder.parsed("team_color", "<${newTeam.color.asHexString()}>"),
            Placeholder.parsed("team_name", newTeam.name.lowercase().replaceFirstChar { it.uppercase() })
        )
        ingamePhase.phaseMode = GamePhaseMode.DICE
    }

    private fun handleArenaPlayerTask(arena: GameArena) {
        val ingamePhase = arena.phase as? IngamePhase ?: return
        val controllingPlayer = ingamePhase.getControllingGamePlayer() ?: return

        // Update bossbar for each player.
        arena.getAllPlayers().forEach { player ->
            updateBossbarForPlayer(player, ingamePhase, controllingPlayer)
        }

        arena.currentPlayers.forEach { gamePlayer ->
            if (gamePlayer.getTeam().deactivated) return@forEach
            if (!ingamePhase.isInControllingTeam(gamePlayer.uuid)) return@forEach

            if (!gamePlayer.isAI && gamePlayer.actionTimeoutTimestamp != null &&
                (ingamePhase.phaseMode == GamePhaseMode.DICE || ingamePhase.phaseMode == GamePhaseMode.PICK_ENTITY)
            ) {
                if (System.currentTimeMillis() >= gamePlayer.actionTimeoutTimestamp!!) {
                    expirePlayerTurn(gamePlayer, ingamePhase)
                    return@forEach
                }
            }

            when (ingamePhase.phaseMode) {
                GamePhaseMode.DICE -> if (gamePlayer.isAI) {
                    // small delay to make the AI-dicing more natural
                    val random: ThreadLocalRandom = ThreadLocalRandom.current()
                    val chanceForLongerDelay: Boolean = random.nextInt(0, 10) > 5
                    val aiDiceDelayTicks: Long = if (chanceForLongerDelay) 20L * (random.nextLong(1L, 3L)) else 1L
                    
                    Bukkit.getScheduler().runTaskLaterAsynchronously(BlockoGame.instance, Runnable {
                        gamePlayer.dice(ingamePhase)
                    }, aiDiceDelayTicks)
                }

                GamePhaseMode.PICK_ENTITY -> {
                    if (gamePlayer.isAI) {
                        gamePlayer.autoPickEntity(ingamePhase)
                    } else {
                        processPlayerEntitySelection(gamePlayer, arena, ingamePhase)
                    }
                }

                GamePhaseMode.MOVE_ENTITY -> gamePlayer.movePickedEntity()
            }
        }
    }

    private fun updateBossbarForPlayer(player: Player, ingamePhase: IngamePhase, controllingPlayer: GamePlayer) {
        if (controllingPlayer.actionTimeoutTimestamp == null) return

        val bossbarHandler: BossbarHandler = BlockoGame.instance.bossbarHandler
        val team: GameTeam = BlockoGame.instance.gameTeamHandler
            .getTeamOfPlayer(controllingPlayer.arenaId, controllingPlayer.uuid) ?: return
        val timeLeft: Long = ingamePhase.getControllingGamePlayerTimeLeft()

        val timePlaceholder = Placeholder.parsed("time", if (timeLeft == 1L) "one" else timeLeft.toString())
        val unitPlaceholder = Placeholder.parsed("unit", if (timeLeft == 1L) "second" else "seconds")
        val timeColor = when {
            timeLeft >= 30 -> NamedTextColor.GREEN.asHexString()
            timeLeft >= 10 -> NamedTextColor.YELLOW.asHexString()
            else -> NamedTextColor.DARK_RED.asHexString()
        }

        val timeColorPlaceholder = Placeholder.parsed("time_color", "<$timeColor>")

        val bossbarText: Component = BlockoGame.instance.translationHandler.getSelectedTranslation().line(
            "blocko.bossbar.timeout",
            Placeholder.parsed("team_color", "<${team.color.asHexString()}>"),
            Placeholder.parsed("team_name", team.name.lowercase().replaceFirstChar { it.uppercase() }),
            timeColorPlaceholder,
            timePlaceholder,
            unitPlaceholder
        )

        if (bossbarHandler.getBossbars(player.uniqueId).none { it.first == Constants.TIMEOUT_BOSSBAR_NAME }) {
            bossbarHandler.registerBossbar(player, Constants.TIMEOUT_BOSSBAR_NAME, bossbarText, 1.0F, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS)
        } else {
            val progress: Float = ingamePhase.getControllingGamePlayerTimeLeftFraction()
            bossbarHandler.updateBossbar(player.uniqueId, Constants.TIMEOUT_BOSSBAR_NAME, BossbarHandler.BossBarUpdate.PROGRESS, progress)
            bossbarHandler.updateBossbar(player.uniqueId, Constants.TIMEOUT_BOSSBAR_NAME, BossbarHandler.BossBarUpdate.NAME, bossbarText)
            val barColor = when {
                timeLeft >= 30 -> BossBar.Color.GREEN
                timeLeft >= 10 -> BossBar.Color.YELLOW
                else -> BossBar.Color.RED
            }
            bossbarHandler.updateBossbar(player.uniqueId, Constants.TIMEOUT_BOSSBAR_NAME, BossbarHandler.BossBarUpdate.COLOR, barColor)
        }
    }

    private fun expirePlayerTurn(gamePlayer: GamePlayer, ingamePhase: IngamePhase) {
        gamePlayer.activeEntity = null
        gamePlayer.lastEntityPickRule = null
        gamePlayer.dicedNumber = null
        gamePlayer.actionTimeoutTimestamp = null

        if (gamePlayer.isDicing())
            BlockoGame.instance.diceHandler.dicingPlayers.remove(gamePlayer.uuid)

        ingamePhase.phaseMode = GamePhaseMode.DICE
        ingamePhase.setNextControllingTeam()

        gamePlayer.translateMessage("blocko.main_game_loop.turn_expired")
        gamePlayer.playSound(Sound.BLOCK_SCULK_SHRIEKER_HIT)
    }

    private fun processPlayerEntitySelection(gamePlayer: GamePlayer, arena: GameArena, ingamePhase: IngamePhase) {
        val dicedNumber = gamePlayer.dicedNumber ?: return
        val teamEntities = BlockoGame.instance.gameEntityHandler.getEntitiesFromTeam(arena.id, gamePlayer.teamName!!)
        
        if (teamEntities.all { (dicedNumber != 6 && it.currentFieldId == null) || !it.isMovableTo(dicedNumber) }) {
            gamePlayer.activeEntity = null
            gamePlayer.lastEntityPickRule = null
            gamePlayer.actionTimeoutTimestamp = null
            BlockoGame.instance.bossbarHandler.unregisterBossbar(gamePlayer.toBukkitInstance()!!, Constants.TIMEOUT_BOSSBAR_NAME)
            ingamePhase.phaseMode = GamePhaseMode.DICE
            ingamePhase.setNextControllingTeam()
        } else {
            gamePlayer.translateActionBar("blocko.main_game_loop.select_entity_notify")
        }
    }

    private fun getHighlightedEntities(gamePlayer: GamePlayer, arena: GameArena): List<GameEntity> {
        return BlockoGame.instance.gameEntityHandler.getEntitiesFromTeam(arena.id, gamePlayer.teamName!!)
            .filter { it.isHighlighted }
    }

    private fun getOtherHighlightedEntities(gamePlayer: GamePlayer, arena: GameArena, highlightedEntity: GameEntity): List<GameEntity> {
        return BlockoGame.instance.gameEntityHandler.getEntitiesFromTeam(arena.id, gamePlayer.teamName!!)
            .filter { it.livingEntity?.uniqueId != highlightedEntity.livingEntity?.uniqueId }
    }
}