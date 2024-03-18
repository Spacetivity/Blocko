package net.spacetivity.blocko.listener

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.extensions.getArena
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.utils.ItemBuilder
import net.spacetivity.blocko.utils.PersistentDataUtils
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.event.server.ServerListPingEvent
import org.bukkit.event.weather.WeatherChangeEvent
import org.bukkit.inventory.ItemStack
import java.util.*

class ProtectionListener(private val plugin: BlockoGame) : Listener {

    init {
        this.plugin.server.pluginManager.registerEvents(this, this.plugin)
    }

    @EventHandler
    fun onServerListPing(event: ServerListPingEvent) {
        val translation: Translation = this.plugin.translationHandler.getSelectedTranslation()

        event.motd(translation.validateLine("blocko.motd"))
    }

    @EventHandler
    fun onInteractWithClickableItem(event: PlayerInteractEvent) {
        val item: ItemStack = event.item ?: return

        if (item.itemMeta == null) return
        if (!PersistentDataUtils.hasData(item.itemMeta, "clickableItem")) return

        val clickableItemId: UUID = PersistentDataUtils.getData(item.itemMeta, "clickableItem", UUID::class.java)

        val itemBuilder: ItemBuilder = this.plugin.clickableItems[clickableItemId] ?: return
        itemBuilder.action.invoke(event)
    }

    @EventHandler
    fun onInteractWithGameEntity(event: PlayerInteractAtEntityEvent) {
        if (BlockoGame.instance.gameEntityHandler.gameEntities.values().none { it.livingEntity!!.uniqueId == event.rightClicked.uniqueId }) return
        event.isCancelled = true
    }

    @EventHandler
    fun onDamageGameEntity(event: EntityDamageByEntityEvent) {
        if (BlockoGame.instance.gameEntityHandler.gameEntities.values().none { it.livingEntity!!.uniqueId == event.entity.uniqueId }) return
        event.isCancelled = true
    }

    @EventHandler
    fun onDamageInArenaWorld(event: EntityDamageEvent) {
        if (event.entity !is Player) return
        val player: Player = event.entity as Player
        if (player.getArena() == null || player.getArena()!!.gameWorld.name != player.world.name) return
        event.isCancelled = true
    }

    @EventHandler
    fun onFoodLevelChangeInArenaWorld(event: FoodLevelChangeEvent) {
        val player: Player = event.entity as Player
        if (player.getArena() == null || player.getArena()!!.gameWorld.name != player.world.name) return
        event.isCancelled = true
    }

    @EventHandler
    fun onSwapItemInArenaWorld(event: PlayerSwapHandItemsEvent) {
        val player: Player = event.player
        if (player.getArena() == null || player.getArena()!!.gameWorld.name != player.world.name) return
        event.isCancelled = true
    }

    @EventHandler
    fun onBlockBreakInArenaWorld(event: BlockBreakEvent) {
        val player: Player = event.player
        if (player.getArena() == null || player.getArena()!!.gameWorld.name != player.world.name) return
        event.isCancelled = true
    }

    @EventHandler
    fun onBlockPlaceInArenaWorld(event: BlockPlaceEvent) {
        val player: Player = event.player
        if (player.getArena() == null || player.getArena()!!.gameWorld.name != player.world.name) return
        event.isCancelled = true
        event.setBuild(false)
    }

    @EventHandler
    fun onItemDragInArenaWorld(event: InventoryDragEvent) {
        val player: Player = event.whoClicked as Player
        if (player.getArena() == null || player.getArena()!!.gameWorld.name != player.world.name) return
        event.isCancelled = true
    }

    @EventHandler
    fun onItemClickInArenaWorld(event: InventoryClickEvent) {
        val player: Player = event.whoClicked as Player
        if (player.getArena() == null || player.getArena()!!.gameWorld.name != player.world.name) return
        event.isCancelled = true
    }

    @EventHandler
    fun onItemDropInArenaWorld(event: PlayerDropItemEvent) {
        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onInteract(event: PlayerInteractEvent) {
        val player: Player = event.player
        if (player.getArena() == null || player.getArena()!!.gameWorld.name != player.world.name) return
        event.isCancelled = true
    }

    @EventHandler
    fun onWeatherChangeInArenaWorld(event: WeatherChangeEvent) {
        if (BlockoGame.instance.gameArenaHandler.cachedArenas.none { it.gameWorld.name == event.world.name }) return
        event.isCancelled = true
    }

    @EventHandler
    fun onEntityCombust(event: EntityCombustEvent) {
        if (BlockoGame.instance.gameArenaHandler.cachedArenas.none { it.gameWorld.name == event.entity.world.name }) return
        event.isCancelled = true
    }

}