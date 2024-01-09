package net.spacetivity.ludo.listener

import net.kyori.adventure.text.Component
import net.spacetivity.ludo.LudoGame
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

class PlayerSetupListener(private val ludoGame: LudoGame) : Listener {

    init {
        this.ludoGame.server.pluginManager.registerEvents(this, this.ludoGame)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player: Player = event.player
        val block: Block = event.clickedBlock ?: return

        if (!this.ludoGame.gameArenaSetupHandler.hasOpenSetup(player.uniqueId)) return
        if (player.inventory.itemInMainHand.type != Material.IRON_HOE) return
        if (event.hand == EquipmentSlot.HAND) return;
        if (event.action.isLeftClick) return
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val validBlockTypes: MutableList<Material> = mutableListOf()

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

        if (!validBlockTypes.contains(block.type)) {
            player.sendMessage(Component.text(warningAlert))
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BASS, 0.1F, 0.1F)
            return
        }

        this.ludoGame.gameArenaSetupHandler.addField(player, block.location)
    }

}