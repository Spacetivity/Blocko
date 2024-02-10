package net.spacetivity.ludo.listener

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.setup.GameArenaSetupData
import net.spacetivity.ludo.arena.setup.GameArenaSetupHandler
import net.spacetivity.ludo.utils.PersistentDataUtils
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

class PlayerSetupListener(private val plugin: LudoGame) : Listener {

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

    @EventHandler
    fun onInteractWithSetupTool(event: PlayerInteractEvent) {
        val player: Player = event.player

        if (event.hand != EquipmentSlot.HAND) return

        val heldItemStack: ItemStack = player.inventory.itemInMainHand

        if (heldItemStack.type == Material.AIR) return
        if (!PersistentDataUtils.hasData(heldItemStack.itemMeta, "setupTool")) return

        val setupData: GameArenaSetupData = this.setupHandler.getSetupData(player.uniqueId) ?: return

        if (event.action.isLeftClick) {
            setupData.setupTool.onToggle(!player.isSneaking, heldItemStack)
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