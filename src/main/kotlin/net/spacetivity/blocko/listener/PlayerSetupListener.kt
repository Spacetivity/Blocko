package net.spacetivity.blocko.listener

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.setup.GameArenaSetupData
import net.spacetivity.blocko.arena.setup.GameArenaSetupHandler
import net.spacetivity.blocko.utils.PersistentDataUtils
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

class PlayerSetupListener(private val plugin: BlockoGame) : Listener {

    private val setupHandler: GameArenaSetupHandler = this.plugin.gameArenaSetupHandler

    init {
        this.plugin.server.pluginManager.registerEvents(this, this.plugin)
    }

    @EventHandler
    fun onQuitWhilstInSetup(event: PlayerQuitEvent) {
        val player: Player = event.player
        val setupData: GameArenaSetupData = this.setupHandler.getSetupData(player.uniqueId) ?: return
        player.inventory.remove(setupData.setupTool.itemStack)
    }

//    @EventHandler
//    fun onBlockBreak(event: BlockBreakEvent) {
//        val player: Player = event.player
//
//        val heldItemStack: ItemStack = player.inventory.itemInMainHand
//        if (heldItemStack.type == Material.AIR) return
//        if (!PersistentDataUtils.hasData(heldItemStack.itemMeta, "setupTool")) return
//
//        event.isCancelled = true
//    }

    @EventHandler
    fun onInteractWithSetupTool(event: PlayerInteractEvent) {
        val player: Player = event.player

        if (event.hand != EquipmentSlot.HAND) return

        val heldItemStack: ItemStack = player.inventory.itemInMainHand

        if (heldItemStack.type == Material.AIR) return
        if (!PersistentDataUtils.hasData(heldItemStack.itemMeta, "setupTool")) return

        val setupData: GameArenaSetupData = this.setupHandler.getSetupData(player.uniqueId) ?: return

        // setup mode changing can only happen when the player is SNEAKING
        if (player.isSneaking) {
            val isNextModeRequested: Boolean = event.action.isLeftClick
            setupData.setupTool.onToggle(isNextModeRequested, heldItemStack)
            return
        }

        if (event.clickedBlock == null) return
        setupData.setupTool.doAction(event)
    }

    @EventHandler
    fun onDropSetupTool(event: PlayerDropItemEvent) {
        if (!PersistentDataUtils.hasData(event.itemDrop.itemStack.itemMeta, "setupTool")) return
        event.isCancelled = true
    }

}