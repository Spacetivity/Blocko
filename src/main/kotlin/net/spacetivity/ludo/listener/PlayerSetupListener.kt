package net.spacetivity.ludo.listener

import net.kyori.adventure.text.Component
import net.spacetivity.inventory.api.SpaceInventoryProvider
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.setup.GameArenaSetupData
import net.spacetivity.ludo.field.GameField
import net.spacetivity.ludo.inventory.GameArenaInventory
import net.spacetivity.ludo.inventory.GameFieldTurnInventory
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
        if (player.inventory.itemInMainHand.type != Material.IRON_HOE) return

        event.isCancelled = true

        val validBlockTypes: MutableList<Material> = mutableListOf()

        validBlockTypes.add(Material.BONE_BLOCK)

        Material.entries.filter { it.name.contains("WOOL") && !it.name.contains("CARPET") }
            .forEach { validBlockTypes.add(it) }

        val warningAlert = "The fields can be wool or bone blocks!"
        doIfBlockIsValid(player, validBlockTypes, block, warningAlert) {
            val arenaSetupData: GameArenaSetupData = this.ludoGame.gameArenaSetupHandler.getSetupData(player.uniqueId)!!

            val gameField: GameField? = arenaSetupData.gameFields
                .find { it.x == block.location.x && it.z == block.location.z && it.world.name == block.location.world.name }

            if (gameField == null) {
                player.sendMessage(Component.text("Not a valid field of arena ${arenaSetupData.arenaId}"))
                return@doIfBlockIsValid
            }

            SpaceInventoryProvider.api.inventoryHandler.openStaticInventory(
                player,
                Component.text("Set a turn"),
                GameFieldTurnInventory(gameField, block.location),
                true
            )
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player: Player = event.player
        val block: Block = event.clickedBlock ?: return

        if (!this.ludoGame.gameArenaSetupHandler.hasOpenSetup(player.uniqueId)) return
        if (player.inventory.itemInMainHand.type != Material.IRON_HOE) return
        if (event.hand == EquipmentSlot.HAND) return;

        val validBlockTypes: MutableList<Material> = mutableListOf()

        if (event.action.isRightClick && event.action == Action.RIGHT_CLICK_BLOCK) {
            val warningAlert: String =
                if (this.ludoGame.gameArenaSetupHandler.hasConfiguredFieldsAlready(player.uniqueId)) {
                    validBlockTypes.add(Material.BONE_BLOCK)
                    Material.entries.filter { it.name.contains("WOOL") && !it.name.contains("CARPET") }
                        .forEach { validBlockTypes.add(it) }
                    "The fields can be wool or bone blocks!"
                } else {
                    Material.entries.filter { it.name.contains("WOOL") && !it.name.contains("CARPET") }
                        .forEach { validBlockTypes.add(it) }
                    "The first field has to be a WOOL block!"
                }

            doIfBlockIsValid(player, validBlockTypes, block, warningAlert) {
                this.ludoGame.gameArenaSetupHandler.addField(player, block.location)
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