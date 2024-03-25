package net.spacetivity.blocko.listener

import io.papermc.paper.event.player.AsyncChatEvent
import io.papermc.paper.event.player.PlayerOpenSignEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.achievement.impl.BadMannersAchievement
import net.spacetivity.blocko.achievement.impl.FairPlayAchievement
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.arena.sign.GameArenaSign
import net.spacetivity.blocko.arena.sign.GameArenaSignHandler
import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.extensions.getArena
import net.spacetivity.blocko.extensions.getTeam
import net.spacetivity.blocko.extensions.toGamePlayerInstance
import net.spacetivity.blocko.extensions.translateMessage
import net.spacetivity.blocko.lobby.LobbySpawn
import net.spacetivity.blocko.phase.GamePhaseMode
import net.spacetivity.blocko.phase.impl.IngamePhase
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.utils.PersistentDataUtils
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Sign
import org.bukkit.block.sign.Side
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scoreboard.Team

class PlayerListener(private val plugin: BlockoGame) : Listener {

    private val gameArenaSignHandler: GameArenaSignHandler = BlockoGame.instance.gameArenaSignHandler

    init {
        this.plugin.server.pluginManager.registerEvents(this, this.plugin)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onJoin(event: PlayerJoinEvent) {
        val player: Player = event.player
        player.allowFlight = true
        player.isFlying = true

        val lobbySpawn: LobbySpawn? = BlockoGame.instance.lobbySpawnHandler.lobbySpawn
        if (lobbySpawn != null) player.teleport(lobbySpawn.toBukkitInstance())

        this.plugin.statsPlayerHandler.createOrLoadStatsPlayer(player.uniqueId)
        this.plugin.achievementHandler.createOrLoadAchievementPlayer(player.uniqueId)
        this.plugin.gameEntityHandler.loadUnlockedEntityTypes(player.uniqueId)

        if (this.plugin.globalConfigFile.gameArenaAutoJoin) {
            val gameArenas: List<GameArena> = this.plugin.gameArenaHandler.cachedArenas
                .filter { !it.isFull() && it.phase.isIdle() }
                .sortedBy { it.currentPlayers.size }
                .reversed()

            if (gameArenas.isEmpty()) {
                player.kick(this.plugin.translationHandler.getSelectedTranslation().validateLine("no_free_arena_found"))
                return
            }

            gameArenas.first().join(player.uniqueId, false)
        }

        this.plugin.playerFormatHandler.setTablistFormatForAll()
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player: Player = event.player
        player.getArena()?.quit(player)
        this.plugin.statsPlayerHandler.unloadStatsPlayer(player.uniqueId)
        this.plugin.achievementHandler.unloadAchievementPlayer(player.uniqueId)
        this.plugin.gameEntityHandler.unloadUnlockedEntityTypes(player.uniqueId)

        for (team: Team in player.scoreboard.teams) {
            if (!team.hasEntry(player.name)) continue
            team.removeEntry(player.name)
        }
    }

    @EventHandler
    fun onChat(event: AsyncChatEvent) {
        val player: Player = event.player

        val rawMessage: String = PlainTextComponentSerializer.plainText().serialize(event.message())
        val gamePlayer: GamePlayer? = player.toGamePlayerInstance()

        if (gamePlayer != null) {
            if (rawMessage.contains("gg", true))
                this.plugin.achievementHandler.getAchievement(FairPlayAchievement::class.java)?.grantIfCompletedBy(gamePlayer)

            if (rawMessage.contains("bg", true))
                this.plugin.achievementHandler.getAchievement(BadMannersAchievement::class.java)?.grantIfCompletedBy(gamePlayer)
        }

        val translation: Translation = this.plugin.translationHandler.getSelectedTranslation()
        val gameArena: GameArena? = player.getArena()

        val isPlaying: Boolean = player.getArena() != null

        val locationPlaceholder: TagResolver.Single = Placeholder.parsed("location", if (isPlaying && gameArena!!.phase.isIdle()) "LOBBY" else if (isPlaying && (gameArena!!.phase.isIngame() || gameArena.phase.isEnding())) "ARENA" else "SERVER")

        val color: NamedTextColor = if (isPlaying) this.plugin.gameTeamHandler.getTeamOfPlayer(gamePlayer!!.arenaId, gamePlayer.uuid)?.color ?: NamedTextColor.GRAY else NamedTextColor.GRAY
        val colorPlaceholder: TagResolver.Single = Placeholder.parsed("color", "<${color.asHexString()}>")

        val namePlaceholder: TagResolver.Single = Placeholder.parsed("player_name", player.name)

        event.viewers().removeIf { isPlaying == (player.getArena() == null) }

        event.renderer { _, _, message, _ ->
            translation.validateLine("blocko.format.chat", locationPlaceholder, colorPlaceholder, namePlaceholder, Placeholder.component("message", message))
        }
    }

    @EventHandler
    fun openSignEvent(event: PlayerOpenSignEvent) {
        val player: Player = event.player

        if (player.inventory.itemInMainHand.type == Material.DIAMOND_HOE || BlockoGame.instance.gameArenaSignHandler.existsLocation(event.sign.location))
            event.isCancelled = true
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val player: Player = event.player
        val block: Block = event.block

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

                val sign: Sign = block.state as Sign
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
        val player: Player = event.player
        val block: Block? = event.clickedBlock

        val itemInHand: ItemStack = player.inventory.itemInMainHand

        when (itemInHand.type) {
            Material.AIR -> {
                if (block == null) return
                if (!event.action.isRightClick && event.action != Action.RIGHT_CLICK_BLOCK) return
                if (!block.type.name.contains("WALL_SIGN", true)) return

                event.isCancelled = true

                val arenaSign: GameArenaSign = BlockoGame.instance.gameArenaSignHandler.getSign(block.location)
                    ?: return
                val gameArena: GameArena? = if (arenaSign.arenaId == null) null else BlockoGame.instance.gameArenaHandler.getArena(arenaSign.arenaId!!)

                if (gameArena == null) {
                    player.translateMessage("blocko.sign.no_arena_assigned")
                    return
                }

                if (player.getArena() != null && player.getArena()!!.id == gameArena.id) {
                    gameArena.quit(player)
                } else if (player.getArena() == null) {
                    gameArena.join(player.uniqueId, false)
                }
            }

            Material.PLAYER_HEAD -> {
                val gameArena: GameArena = player.getArena() ?: return
                if (!gameArena.phase.isIngame()) return

                if (!event.action.isRightClick) return

                val gamePlayer: GamePlayer = player.toGamePlayerInstance() ?: return

                if (gamePlayer.getTeam().deactivated) {
                    player.translateMessage("blocko.main_game_loop.already_saved_all_entities")
                    return
                }

                val ingamePhase: IngamePhase = gameArena.phase as IngamePhase

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

                val gameArena: GameArena = player.getArena() ?: return
                if (!gameArena.phase.isIngame()) return

                val ingamePhase: IngamePhase = gameArena.phase as IngamePhase
                val gamePlayer: GamePlayer = player.toGamePlayerInstance() ?: return

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

                if (!PersistentDataUtils.hasData(itemInHand.itemMeta, "entitySelector")) return

                val entityId: Int = PersistentDataUtils.getData(itemInHand.itemMeta, "entitySelector", Int::class.java)
                val gameEntity: GameEntity = BlockoGame.instance.gameEntityHandler.getEntitiesFromTeam(gameArena.id, gamePlayer.teamName!!).find { it.entityId == entityId }
                    ?: return

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