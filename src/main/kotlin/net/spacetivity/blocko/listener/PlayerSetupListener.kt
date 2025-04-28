package net.spacetivity.blocko.listener

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.setup.GameArenaSetupHandler
import net.spacetivity.blocko.arena.setup.GameArenaSetupSession
import net.spacetivity.blocko.utils.Constants
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
    fun onQuitWhileInSetup(event: PlayerQuitEvent) {
        val player: Player = event.player
        val setupData: GameArenaSetupSession = this.setupHandler.getSetupData(player.uniqueId) ?: return

        this.setupHandler.handleSetupEnd(player, false)

        player.inventory.removeAll { PersistentDataUtils.has(it.itemMeta, Constants.SETUP_TOOL_KEY) }
        player.inventory.remove(setupData.setupTool.itemStack)
    }

    @EventHandler
    fun onInteractWithSetupTool(event: PlayerInteractEvent) {
        val player: Player = event.player

        if (event.hand != EquipmentSlot.HAND) return

        val heldItemStack: ItemStack = player.inventory.itemInMainHand
        if (heldItemStack.type == Material.AIR) return
        if (!PersistentDataUtils.has(heldItemStack.itemMeta, Constants.SETUP_TOOL_KEY)) return

        val setupSession: GameArenaSetupSession = this.setupHandler.getSetupData(player.uniqueId) ?: return

        // setup mode changing can only happen when the player is SNEAKING
        if (player.isSneaking) {
            val isNextModeRequested: Boolean = event.action.isLeftClick
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