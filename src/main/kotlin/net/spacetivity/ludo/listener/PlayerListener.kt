package net.spacetivity.ludo.listener

import io.papermc.paper.event.player.PlayerOpenSignEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.inventory.api.SpaceInventoryProvider
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.arena.setup.GameArenaSetupData
import net.spacetivity.ludo.arena.sign.GameArenaSign
import net.spacetivity.ludo.arena.sign.GameArenaSignHandler
import net.spacetivity.ludo.extensions.getArena
import net.spacetivity.ludo.extensions.isDicing
import net.spacetivity.ludo.extensions.startDicing
import net.spacetivity.ludo.extensions.stopDicing
import net.spacetivity.ludo.field.GameField
import net.spacetivity.ludo.inventory.GameFieldTurnSetupInventory
import net.spacetivity.ludo.inventory.GameTeamSetupInventory
import net.spacetivity.ludo.inventory.InvType
import net.spacetivity.ludo.phase.GamePhaseMode
import net.spacetivity.ludo.phase.impl.IngamePhase
import net.spacetivity.ludo.utils.MetadataUtils
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.Sign
import org.bukkit.block.sign.Side
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerListener(private val ludoGame: LudoGame) : Listener {

    private val gameArenaSignHandler: GameArenaSignHandler = LudoGame.instance.gameArenaSignHandler

    init {
        this.ludoGame.server.pluginManager.registerEvents(this, this.ludoGame)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player: Player = event.player
        player.getArena()?.quit(player)
        MetadataUtils.remove(player, "dicedNumber")
    }

    @EventHandler
    fun onKick(event: PlayerKickEvent) {
        val player: Player = event.player
        player.getArena()?.quit(player)
        MetadataUtils.remove(player, "dicedNumber")
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val player: Player = event.player
        val block: Block = event.block

        val validBlockTypes: MutableList<Material> = mutableListOf(Material.BONE_BLOCK)

        for (material: Material in Material.entries.filter { it.name.contains("WOOL") && !it.name.contains("CARPET") })
            validBlockTypes.add(material)

        when (player.inventory.itemInMainHand.type) {

            Material.IRON_HOE -> {

                if (!this.ludoGame.gameArenaSetupHandler.hasOpenSetup(player.uniqueId)) return
                event.isCancelled = true

                if (!MetadataUtils.has(player, "fieldsFinished")) {
                    player.sendMessage(Component.text("Set all game fields first, before you set the garage fields!"))
                    return
                }

                doIfBlockIsValid(player, validBlockTypes, block, "The fields can be wool or bone blocks!") {
                    val arenaSetupData: GameArenaSetupData = this.ludoGame.gameArenaSetupHandler.getSetupData(player.uniqueId)!!
                    val gameField: GameField? = arenaSetupData.gameFields.find { it.x == block.location.x && it.z == block.location.z && it.world.name == block.location.world.name }

                    if (gameField == null) {
                        player.sendMessage(Component.text("Not a valid field of arena ${arenaSetupData.arenaId}"))
                        return@doIfBlockIsValid
                    }

                    SpaceInventoryProvider.api.inventoryHandler.openStaticInventory(player, Component.text("Set a turn"), GameFieldTurnSetupInventory(gameField, block.location), true)
                }

            }

            Material.GOLDEN_HOE -> {

                if (!this.ludoGame.gameArenaSetupHandler.hasOpenSetup(player.uniqueId)) return
                event.isCancelled = true

                if (!MetadataUtils.has(player, "fieldsFinished")) {
                    player.sendMessage(Component.text("Set all game fields first, before you set the garage fields!"))
                    return
                }

                doIfBlockIsValid(player, mutableListOf(Material.BONE_BLOCK), block, "A team entrance has to be a BONE block!") {
                    SpaceInventoryProvider.api.inventoryHandler.openStaticInventory(
                        player,
                        Component.text("Set team entrance"),
                        GameTeamSetupInventory(InvType.ENTRANCE, block.location),
                        true
                    )
                }

            }

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

    @EventHandler
    fun openSignEvent(event: PlayerOpenSignEvent) {
        val player: Player = event.player

        if (player.inventory.itemInMainHand.type == Material.DIAMOND_HOE || LudoGame.instance.gameArenaSignHandler.existsLocation(event.sign.location))
            event.isCancelled = true
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player: Player = event.player
        val block: Block? = event.clickedBlock

        val validBlockTypes: MutableList<Material> = mutableListOf()
        val warningAlert: String

        when (player.inventory.itemInMainHand.type) {
            Material.AIR -> {
                if (block == null) return
                if (!event.action.isRightClick && event.action != Action.RIGHT_CLICK_BLOCK) return
                if (!block.type.name.contains("WALL_SIGN", true)) return
                event.isCancelled = true

                val arenaSign: GameArenaSign = LudoGame.instance.gameArenaSignHandler.getSign(block.location) ?: return
                val gameArena: GameArena? = if (arenaSign.arenaId == null) null else LudoGame.instance.gameArenaHandler.getArena(arenaSign.arenaId!!)

                if (gameArena == null) {
                    player.sendMessage(Component.text("This sign has no arena assigned!", NamedTextColor.DARK_RED))
                    return
                }

                if (player.getArena() != null && player.getArena()!!.id == gameArena.id) gameArena.quit(player)
                else gameArena.join(player)
            }

            Material.PLAYER_HEAD -> {
                val gameArena: GameArena = player.getArena() ?: return
                if (!gameArena.phase.isIngame()) return

                if (!event.action.isRightClick) return

                val ingamePhase: IngamePhase = gameArena.phase as IngamePhase

                if (!ingamePhase.isInControllingTeam(player.uniqueId)) {
                    player.sendMessage(Component.text("Please wait your turn!", NamedTextColor.RED))
                    return
                }

                if (ingamePhase.phaseMode != GamePhaseMode.DICING) {
                    player.sendMessage(Component.text("You cannot dice now!", NamedTextColor.RED))
                    return
                }

                if (player.isDicing()) player.stopDicing()
                else player.startDicing()
            }

            Material.IRON_HOE -> {

                if (block == null) return
                if (!event.action.isRightClick && event.action != Action.RIGHT_CLICK_BLOCK) return
                if (!this.ludoGame.gameArenaSetupHandler.hasOpenSetup(player.uniqueId)) return

                if (this.ludoGame.gameArenaSetupHandler.hasConfiguredFieldsAlready(player.uniqueId)) {
                    for (material: Material in Material.entries.filter { it.name.contains("WOOL") && !it.name.contains("CARPET") })
                        validBlockTypes.add(material)

                    validBlockTypes.add(Material.BONE_BLOCK)
                    warningAlert = "The fields can be wool or bone blocks!"
                } else {
                    for (material: Material in Material.entries.filter { it.name.contains("WOOL") && !it.name.contains("CARPET") })
                        validBlockTypes.add(material)

                    warningAlert = "The first field has to be a WOOL block!"
                }

                doIfBlockIsValid(player, validBlockTypes, block, warningAlert) {
                    this.ludoGame.gameArenaSetupHandler.addField(player, block.location)
                }

            }

            Material.GOLDEN_HOE -> {

                if (block == null) return
                if (!event.action.isRightClick && event.action != Action.RIGHT_CLICK_BLOCK) return
                if (!this.ludoGame.gameArenaSetupHandler.hasOpenSetup(player.uniqueId)) return

                if (!MetadataUtils.has(player, "fieldsFinished")) {
                    player.sendMessage(Component.text("Set all game fields first, before you set the garage fields!"))
                    return
                }

                for (material: Material in Material.entries.filter { it.name.contains("WOOL") && !it.name.contains("CARPET") })
                    validBlockTypes.add(material)

                warningAlert = "A garage field has to be a WOOL block!"

                doIfBlockIsValid(player, validBlockTypes, block, warningAlert) {
                    SpaceInventoryProvider.api.inventoryHandler.openStaticInventory(
                        player,
                        Component.text("Add garage field"),
                        GameTeamSetupInventory(InvType.GARAGE, block.location),
                        true
                    )
                }

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

    private fun doIfBlockIsValid(player: Player, validBlockTypes: MutableList<Material>, block: Block, warningAlert: String, result: () -> (Unit)) {
        if (!validBlockTypes.contains(block.type)) {
            player.sendMessage(Component.text(warningAlert))
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BASS, 0.1F, 0.1F)
            return
        }

        result.invoke()
    }

}