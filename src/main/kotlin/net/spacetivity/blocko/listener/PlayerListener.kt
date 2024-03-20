package net.spacetivity.blocko.listener

import io.papermc.paper.event.player.AsyncChatEvent
import io.papermc.paper.event.player.PlayerOpenSignEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.achievement.container.Achievement
import net.spacetivity.blocko.achievement.impl.FairPlayAchievement
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.arena.sign.GameArenaSign
import net.spacetivity.blocko.arena.sign.GameArenaSignHandler
import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.extensions.getArena
import net.spacetivity.blocko.extensions.getTeam
import net.spacetivity.blocko.extensions.toGamePlayerInstance
import net.spacetivity.blocko.phase.GamePhaseMode
import net.spacetivity.blocko.phase.impl.IngamePhase
import net.spacetivity.blocko.player.GamePlayer
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

class PlayerListener(private val plugin: BlockoGame) : Listener {

    private val gameArenaSignHandler: GameArenaSignHandler = BlockoGame.instance.gameArenaSignHandler

    init {
        this.plugin.server.pluginManager.registerEvents(this, this.plugin)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onJoin(event: PlayerJoinEvent) {
        val player: Player = event.player
        this.plugin.statsPlayerHandler.createOrLoadStatsPlayer(player.uniqueId)
        this.plugin.achievementHandler.createOrLoadAchievementPlayer(player.uniqueId)
        this.plugin.gameEntityHandler.loadUnlockedEntityTypes(player.uniqueId)

        if (this.plugin.globalConfigFile.gameArenaAutoJoin) {
            val gameArenas: List<GameArena> = this.plugin.gameArenaHandler.cachedArenas
                .filter { !it.isFull() && it.phase.isIdle() }
                .sortedBy { it.currentPlayers.size }
                .reversed()

            if (gameArenas.isEmpty()) {
                player.kick(Component.text("No free arenas found!", NamedTextColor.RED))
                return
            }

            gameArenas.first().join(player.uniqueId, false)
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player: Player = event.player
        player.getArena()?.quit(player)
        this.plugin.statsPlayerHandler.unloadStatsPlayer(player.uniqueId)
        this.plugin.achievementHandler.unloadAchievementPlayer(player.uniqueId)
        this.plugin.gameEntityHandler.unloadUnlockedEntityTypes(player.uniqueId)
    }

    @EventHandler
    fun onChat(event: AsyncChatEvent) {
        val gamePlayer: GamePlayer = event.player.toGamePlayerInstance() ?: return

        val rawMessage: String = PlainTextComponentSerializer.plainText().serialize(event.message())
        if (!rawMessage.contains("gg", true)) return

        val achievement: Achievement = this.plugin.achievementHandler.getAchievement(FairPlayAchievement::class.java) ?: return
        achievement.grantIfCompletedBy(gamePlayer)
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
                    player.sendMessage(Component.text("You only can create a arena sign on a wall sign block!"))
                    return
                }

                event.isCancelled = true

                if (!this.gameArenaSignHandler.existsLocation(block.location)) {
                    player.sendMessage(Component.text("At this location is no arena sign!"))
                    return
                }

                this.gameArenaSignHandler.deleteArenaSign(block.location)

                val sign: Sign = block.state as Sign
                sign.getSide(Side.FRONT).line(0, Component.text(""))
                sign.getSide(Side.FRONT).line(1, Component.text(""))
                sign.getSide(Side.FRONT).line(2, Component.text(""))
                sign.getSide(Side.FRONT).line(3, Component.text(""))
                sign.update()

                player.sendMessage(Component.text("Deleted arena sign!"))
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

                val arenaSign: GameArenaSign = BlockoGame.instance.gameArenaSignHandler.getSign(block.location) ?: return
                val gameArena: GameArena? = if (arenaSign.arenaId == null) null else BlockoGame.instance.gameArenaHandler.getArena(arenaSign.arenaId!!)

                if (gameArena == null) {
                    player.sendMessage(Component.text("This sign has no arena assigned!", NamedTextColor.DARK_RED))
                    return
                }

                if (player.getArena() != null && player.getArena()!!.id == gameArena.id) {
                    gameArena.quit(player)
                } else if (player.getArena() == null) {
                    gameArena.join(player.uniqueId, false)
                } else {
                    player.sendMessage(Component.text("Error : No sign action found...", NamedTextColor.DARK_RED))
                }
            }

            Material.PLAYER_HEAD -> {
                val gameArena: GameArena = player.getArena() ?: return
                if (!gameArena.phase.isIngame()) return

                if (!event.action.isRightClick) return

                val gamePlayer: GamePlayer = player.toGamePlayerInstance() ?: return

                if (gamePlayer.getTeam().deactivated) {
                    player.sendMessage(Component.text("You have already saved all your entities!", NamedTextColor.GOLD))
                    return
                }

                val ingamePhase: IngamePhase = gameArena.phase as IngamePhase

                if (!ingamePhase.isInControllingTeam(gamePlayer.uuid)) {
                    player.sendMessage(Component.text("Please wait your turn!", NamedTextColor.RED))
                    return
                }

                if (ingamePhase.phaseMode != GamePhaseMode.DICE) {
                    player.sendMessage(Component.text("You cannot dice now (${ingamePhase.phaseMode.name})!", NamedTextColor.RED))
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
                    player.sendMessage(Component.text("You have already saved all your entities!", NamedTextColor.GOLD))
                    return
                }

                if (!ingamePhase.isInControllingTeam(gamePlayer.uuid) || ingamePhase.phaseMode != GamePhaseMode.PICK_ENTITY) {
                    player.sendMessage(Component.text("You cannot pick an entity now!", NamedTextColor.RED))
                    return
                }

                if (!PersistentDataUtils.hasData(itemInHand.itemMeta, "entitySelector")) return

                val entityId: Int = PersistentDataUtils.getData(itemInHand.itemMeta, "entitySelector", Int::class.java)
                val gameEntity: GameEntity = BlockoGame.instance.gameEntityHandler.getEntitiesFromTeam(gameArena.id, gamePlayer.teamName!!).find { it.entityId == entityId }
                    ?: return

                if (gamePlayer.dicedNumber!! != 6 && gameEntity.currentFieldId == null) {
                    player.sendMessage(Component.text("You cannot move this entity into the field without dicing 6 first!", NamedTextColor.RED))
                    return
                }

                if (!gameEntity.isMovableTo(gamePlayer.dicedNumber!!)) {
                    player.sendMessage(Component.text("You cannot move this entity!"))
                    return
                }

                gamePlayer.manuallyPickEntity(ingamePhase, gameEntity)

                getOtherHighlightedEntities(gamePlayer, gameArena, gameEntity).forEach { it.toggleHighlighting(false) }

                val dicedNumber: Int = gamePlayer.dicedNumber ?: return
                player.sendMessage(Component.text("Game entity selected to move $dicedNumber fields."))
            }

            Material.DIAMOND_HOE -> {
                if (block == null) return
                if (!event.action.isRightClick && event.action != Action.RIGHT_CLICK_BLOCK) return

                if (!block.type.name.contains("WALL_SIGN", true)) {
                    player.sendMessage(Component.text("You only can create a arena sign on a wall sign block!"))
                    return
                }

                if (this.gameArenaSignHandler.existsLocation(block.location)) {
                    player.sendMessage(Component.text("At this location is already a arena sign!"))
                    return
                }

                this.gameArenaSignHandler.createSignLocation(block.location)
                this.gameArenaSignHandler.loadArenaSigns()

                player.sendMessage(Component.text("Added arena join sign!", NamedTextColor.GREEN))
            }

            else -> {}
        }
    }

    private fun getOtherHighlightedEntities(gamePlayer: GamePlayer, gameArena: GameArena, highlightedEntity: GameEntity): List<GameEntity> {
        return BlockoGame.instance.gameEntityHandler.getEntitiesFromTeam(gameArena.id, gamePlayer.teamName!!).filter { it.livingEntity?.uniqueId != highlightedEntity.livingEntity?.uniqueId }
    }

}