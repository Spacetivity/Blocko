package net.spacetivity.ludo.listener

import net.kyori.adventure.text.Component
import net.spacetivity.inventory.api.SpaceInventoryProvider
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.setup.GameArenaSetupData
import net.spacetivity.ludo.field.GameField
import net.spacetivity.ludo.inventory.GameArenaInventory
import net.spacetivity.ludo.inventory.GameFieldTurnSetupInventory
import net.spacetivity.ludo.inventory.GameTeamSetupInventory
import net.spacetivity.ludo.inventory.InvType
import net.spacetivity.ludo.utils.MetadataUtils
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.EquipmentSlot

class PlayerSetupListener(private val ludoGame: LudoGame) : Listener {

    init {
        this.ludoGame.server.pluginManager.registerEvents(this, this.ludoGame)
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player: Player = event.player

        if (player.hasPermission("ludo.command")) {
            SpaceInventoryProvider.api.inventoryHandler.cacheInventory(
                player,
                Component.text("Game arenas"),
                GameArenaInventory()
            )
        }

    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val player: Player = event.player
        val block: Block = event.block

        if (!this.ludoGame.gameArenaSetupHandler.hasOpenSetup(player.uniqueId)) return

        event.isCancelled = true

        val validBlockTypes: MutableList<Material> = mutableListOf(Material.BONE_BLOCK)

        for (material: Material in Material.entries.filter { it.name.contains("WOOL") && !it.name.contains("CARPET") })
            validBlockTypes.add(material)

        if (player.inventory.itemInMainHand.type == Material.IRON_HOE) {

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

        } else if (player.inventory.itemInMainHand.type == Material.GOLDEN_HOE) {

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
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player: Player = event.player
        val block: Block = event.clickedBlock ?: return

        if (!this.ludoGame.gameArenaSetupHandler.hasOpenSetup(player.uniqueId)) return
        if (event.hand == EquipmentSlot.HAND) return;

        val validBlockTypes: MutableList<Material> = mutableListOf()
        if (!event.action.isRightClick && event.action != Action.RIGHT_CLICK_BLOCK) return

        val warningAlert: String

        if (player.inventory.itemInMainHand.type == Material.IRON_HOE) {

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

        } else if (player.inventory.itemInMainHand.type == Material.GOLDEN_HOE) {

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