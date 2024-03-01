package net.spacetivity.ludo.listener

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.extensions.getArena
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.event.weather.WeatherChangeEvent

class ProtectionListener(private val plugin: LudoGame) : Listener {

    init {
        this.plugin.server.pluginManager.registerEvents(this, this.plugin)
    }

    @EventHandler
    fun onInteractWithGameEntity(event: PlayerInteractAtEntityEvent) {
        if (LudoGame.instance.gameEntityHandler.gameEntities.values().none { it.livingEntity!!.uniqueId == event.rightClicked.uniqueId }) return
        event.isCancelled = true
    }

    @EventHandler
    fun onDamageGameEntity(event: EntityDamageByEntityEvent) {
        if (LudoGame.instance.gameEntityHandler.gameEntities.values().none { it.livingEntity!!.uniqueId == event.entity.uniqueId }) return
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
        val player: Player = event.player
        if (player.getArena() == null || player.getArena()!!.gameWorld.name != player.world.name) return
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
        if (LudoGame.instance.gameArenaHandler.cachedArenas.none { it.gameWorld.name == event.world.name }) return
        event.isCancelled = true
    }

}