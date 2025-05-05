package net.spacetivity.blocko.listener

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.setup.getSetupSession
import net.spacetivity.blocko.utils.Constants
import net.spacetivity.blocko.utils.PersistentDataUtils
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot

class PlayerSetupListener(private val plugin: BlockoGame) : Listener {

    private val setupHandler = this.plugin.arenaSetupHandler

    init {
        this.plugin.server.pluginManager.registerEvents(this, this.plugin)
    }

    @EventHandler
    fun onQuitWhileInSetup(event: PlayerQuitEvent) {
        this.setupHandler.handleSetupEnd(event.player, false)
    }

    @EventHandler
    fun onInteractWithSetupTool(event: PlayerInteractEvent) {
        val player = event.player

        if (event.hand != EquipmentSlot.HAND) return

        val heldItemStack = player.inventory.itemInMainHand
        if (heldItemStack.type == Material.AIR) return
        if (!PersistentDataUtils.has(heldItemStack.itemMeta, Constants.SETUP_TOOL_KEY)) return

        val setupSession = player.getSetupSession() ?: return

        // setup mode changing can only happen when the player is SNEAKING
        if (player.isSneaking) {
            val isNextModeRequested = event.action.isLeftClick
            setupSession.setupTool.onToggle(isNextModeRequested, heldItemStack)
            return
        }

        if (event.clickedBlock == null) return
        setupSession.setupTool.doAction(event)
    }

    @EventHandler
    fun onDropSetupTool(event: PlayerDropItemEvent) {
        if (!PersistentDataUtils.has(event.itemDrop.itemStack.itemMeta, Constants.SETUP_TOOL_KEY)) return
        event.isCancelled = true
    }

}