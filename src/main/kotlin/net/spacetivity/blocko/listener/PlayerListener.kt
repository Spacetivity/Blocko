package net.spacetivity.blocko.listener

import io.papermc.paper.event.player.AsyncChatEvent
import io.papermc.paper.event.player.PlayerOpenSignEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.achievement.grantIfCompletedBy
import net.spacetivity.blocko.achievement.impl.BadMannersAchievement
import net.spacetivity.blocko.achievement.impl.FairPlayAchievement
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.arena.getArena
import net.spacetivity.blocko.arena.toGamePlayerInstance
import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.phase.GamePhaseMode
import net.spacetivity.blocko.phase.impl.IngamePhase
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.player.getTeam
import net.spacetivity.blocko.translation.translateMessage
import net.spacetivity.blocko.utils.Constants.ENTITY_SELECTOR_KEY
import net.spacetivity.blocko.utils.PersistentDataUtils
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.Sign
import org.bukkit.block.sign.Side
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerListener(private val plugin: BlockoGame) : Listener {

    private val gameArenaSignHandler = BlockoGame.instance.gameArenaSignHandler

    init {
        this.plugin.server.pluginManager.registerEvents(this, this.plugin)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        player.gameMode = GameMode.ADVENTURE
        player.allowFlight = true
        player.isFlying = true

        val lobbySpawn = BlockoGame.instance.lobbySpawnHandler.lobbySpawn
        if (lobbySpawn != null) player.teleport(lobbySpawn.toBukkitInstance())

        this.plugin.statsPlayerHandler.createOrLoadStatsPlayer(player.uniqueId)
        this.plugin.achievementHandler.createOrLoadAchievementPlayer(player.uniqueId)
        this.plugin.gameEntityHandler.loadUnlockedEntityTypes(player.uniqueId)
        this.plugin.gameEntityHandler.loadGameEntityHistory(player.uniqueId)

        for (currentPlayer in Bukkit.getOnlinePlayers()) {
            if (currentPlayer.getArena() != null) {
                currentPlayer.hidePlayer(this.plugin, player)
                player.hidePlayer(this.plugin, currentPlayer)
            }
        }

        if (this.plugin.globalConfigFile.gameArenaAutoJoin) {
            val gameArenas = this.plugin.gameArenaHandler.cachedArenas
                .filter { !it.isFull() && it.phase.isIdle() }
                .sortedBy { it.currentPlayers.size }
                .reversed()

            if (gameArenas.isEmpty()) {
                player.kick(this.plugin.translationHandler.getSelectedTranslation().line("no_free_arena_found"))
                return
            }

            gameArenas.first().join(player.uniqueId, false)
        }

        this.plugin.playerFormatHandler.setTablistFormatForAll()
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        player.getArena()?.quit(player)
        this.plugin.statsPlayerHandler.unloadStatsPlayer(player.uniqueId)
        this.plugin.achievementHandler.unloadAchievementPlayer(player.uniqueId)
        this.plugin.gameEntityHandler.unloadUnlockedEntityTypes(player.uniqueId)
        this.plugin.gameEntityHandler.unloadGameEntityHistory(player.uniqueId)

        for (team in player.scoreboard.teams) {
            if (!team.hasEntry(player.name)) continue
            team.removeEntry(player.name)
        }
    }

    @EventHandler
    fun onChat(event: AsyncChatEvent) {
        val player = event.player

        val rawMessage = PlainTextComponentSerializer.plainText().serialize(event.message())
        val gamePlayer = player.toGamePlayerInstance()

        if (gamePlayer != null) {
            if (rawMessage.contains("gg", true)) gamePlayer.grantIfCompletedBy(FairPlayAchievement::class)
            if (rawMessage.contains("bg", true)) gamePlayer.grantIfCompletedBy(BadMannersAchievement::class)
        }

        val translation = this.plugin.translationHandler.getSelectedTranslation()
        val gameArena = player.getArena()

        val isPlaying = player.getArena() != null

        val locationPlaceholder = Placeholder.parsed("location", if (isPlaying && gameArena!!.phase.isIdle()) "LOBBY" else if (isPlaying && (gameArena!!.phase.isIngame() || gameArena.phase.isEnding())) "ARENA" else "SERVER")

        val color = if (isPlaying) this.plugin.gameTeamHandler.getTeamOfPlayer(gamePlayer!!.arenaId, gamePlayer.uuid)?.color
            ?: NamedTextColor.GRAY else NamedTextColor.GRAY
        val colorPlaceholder = Placeholder.parsed("color", "<${color.asHexString()}>")

        val namePlaceholder = Placeholder.parsed("player_name", player.name)

        event.viewers().removeIf { isPlaying == (player.getArena() == null) }

        event.renderer { _, _, message, _ ->
            translation.line("blocko.format.chat", locationPlaceholder, colorPlaceholder, namePlaceholder, Placeholder.component("message", message))
        }
    }

    @EventHandler
    fun openSignEvent(event: PlayerOpenSignEvent) {
        val player = event.player

        if (player.inventory.itemInMainHand.type == Material.DIAMOND_HOE || BlockoGame.instance.gameArenaSignHandler.existsLocation(event.sign.location))
            event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onBreak(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block

        when (player.inventory.itemInMainHand.type) {

            Material.DIAMOND_HOE -> {
                if (!block.type.name.contains("WALL_SIGN", true)) {
                    player.translateMessage("blocko.sign.cannot_create_at_invalid_block")
                    return
                }

                event.isCancelled = true

                if (!this.gameArenaSignHandler.existsLocation(block.location)) {
                    player.translateMessage("blocko.sign.not_found")
                    return
                }

                this.gameArenaSignHandler.deleteArenaSign(block.location)

                val sign = block.state as Sign
                sign.getSide(Side.FRONT).line(0, Component.text(""))
                sign.getSide(Side.FRONT).line(1, Component.text(""))
                sign.getSide(Side.FRONT).line(2, Component.text(""))
                sign.getSide(Side.FRONT).line(3, Component.text(""))
                sign.update()

                player.translateMessage("blocko.sign.deleted")
            }

            else -> {}

        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        val block = event.clickedBlock

        val itemInHand = player.inventory.itemInMainHand

        when (itemInHand.type) {
            Material.AIR -> {
                if (block == null) return
                if (!event.action.isRightClick && event.action != Action.RIGHT_CLICK_BLOCK) return
                if (!block.type.name.contains("WALL_SIGN", true)) return

                event.isCancelled = true

                val arenaSign = BlockoGame.instance.gameArenaSignHandler.getSign(block.location) ?: return
                val gameArena = if (arenaSign.arenaId == null) null else BlockoGame.instance.gameArenaHandler.getArena(arenaSign.arenaId!!)

                if (gameArena == null) {
                    player.translateMessage("blocko.sign.no_arena_assigned")
                    return
                }

                if (player.getArena() != null && player.getArena()!!.id == gameArena.id) {
                    gameArena.quit(player)
                } else if (player.getArena() == null) {
                    if (gameArena.phase.isIngame()) {
                        gameArena.joinAsSpectator(player)
                    } else {
                        gameArena.join(player.uniqueId, false)
                    }
                }
            }

            Material.PLAYER_HEAD -> {
                val gameArena = player.getArena() ?: return
                if (!gameArena.phase.isIngame()) return

                if (!event.action.isRightClick) return

                val gamePlayer = player.toGamePlayerInstance() ?: return

                if (gamePlayer.getTeam().deactivated) {
                    player.translateMessage("blocko.main_game_loop.already_saved_all_entities")
                    return
                }

                val ingamePhase = gameArena.phase as IngamePhase

                if (!ingamePhase.isInControllingTeam(gamePlayer.uuid)) {
                    player.translateMessage("blocko.main_game_loop.wrong_turn")
                    return
                }

                if (ingamePhase.phaseMode != GamePhaseMode.DICE) {
                    player.translateMessage("blocko.main_game_loop.can_not_dice")
                    return
                }

                gamePlayer.dice(ingamePhase)
            }

            Material.ARMOR_STAND -> {
                if (!event.action.isRightClick) return

                val gameArena = player.getArena() ?: return
                if (!gameArena.phase.isIngame()) return

                val ingamePhase = gameArena.phase as IngamePhase
                val gamePlayer = player.toGamePlayerInstance() ?: return

                if (gamePlayer.getTeam().deactivated) {
                    player.translateMessage("blocko.main_game_loop.already_saved_all_entities")
                    return
                }

                if (!ingamePhase.isInControllingTeam(gamePlayer.uuid)) {
                    player.translateMessage("blocko.main_game_loop.wrong_turn")
                    return
                }

                if (ingamePhase.phaseMode != GamePhaseMode.PICK_ENTITY) {
                    player.translateMessage("blocko.main_game_loop.cannot_pick_entity_now")
                    return
                }

                if (!PersistentDataUtils.has(itemInHand.itemMeta, ENTITY_SELECTOR_KEY)) return

                val entityId = PersistentDataUtils.get(itemInHand.itemMeta, ENTITY_SELECTOR_KEY, Int::class.java)
                val gameEntity = BlockoGame.instance.gameEntityHandler.getEntitiesFromTeam(gameArena.id, gamePlayer.teamName!!).find { it.entityId == entityId } ?: return

                if (gamePlayer.dicedNumber!! != 6 && gameEntity.currentFieldId == null) {
                    player.translateMessage("blocko.main_game_loop.needs_a_six")
                    return
                }

                if (!gameEntity.isMovableTo(gamePlayer.dicedNumber!!)) {
                    player.translateMessage("blocko.main_game_loop.entity_not_movable")
                    return
                }

                gamePlayer.manuallyPickEntity(ingamePhase, gameEntity)

                getOtherHighlightedEntities(gamePlayer, gameArena, gameEntity).forEach { it.toggleHighlighting(false) }

                player.translateMessage("blocko.main_game_loop.entity_selected")
            }

            Material.DIAMOND_HOE -> {
                if (block == null) return
                if (!event.action.isRightClick && event.action != Action.RIGHT_CLICK_BLOCK) return

                if (!block.type.name.contains("WALL_SIGN", true)) {
                    player.translateMessage("blocko.sign.cannot_create_at_invalid_block")
                    return
                }

                if (this.gameArenaSignHandler.existsLocation(block.location)) {
                    player.translateMessage("blocko.sign.already_exists")
                    return
                }

                this.gameArenaSignHandler.createSignLocation(block.location)
                this.gameArenaSignHandler.loadArenaSigns()

                player.translateMessage("blocko.sign.created")
            }

            else -> {}
        }
    }

    private fun getOtherHighlightedEntities(gamePlayer: GamePlayer, gameArena: GameArena, highlightedEntity: GameEntity): List<GameEntity> {
        return BlockoGame.instance.gameEntityHandler.getEntitiesFromTeam(gameArena.id, gamePlayer.teamName!!).filter { it.livingEntity?.uniqueId != highlightedEntity.livingEntity?.uniqueId }
    }

}