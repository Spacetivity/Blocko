package net.spacetivity.blocko.listener

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.utils.Constants
import net.spacetivity.blocko.utils.PersistentDataUtils
import org.bukkit.GameMode
import org.bukkit.World
import org.bukkit.entity.Chicken
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.*
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.event.server.ServerListPingEvent
import org.bukkit.event.weather.WeatherChangeEvent
import java.util.*

class ProtectionListener(private val plugin: BlockoGame) : Listener {

    init {
        this.plugin.server.pluginManager.registerEvents(this, this.plugin)
    }

    @EventHandler
    fun onServerListPing(event: ServerListPingEvent) {
        if (!this.plugin.globalConfigFile.motdEnabled) return
        event.motd(this.plugin.translationHandler.getSelectedTranslation().line("blocko.motd"))
    }

    @EventHandler
    fun onInteractWithInteractiveItemStack(event: PlayerInteractEvent) {
        val itemMeta = event.item?.itemMeta ?: return
        val interactiveItemId = PersistentDataUtils.get(itemMeta, Constants.INTERACTIVE_ITEMSTACK_KEY, UUID::class.java)
        BlockoGame.instance.interactiveActions[interactiveItemId]?.invoke(event)
    }

    @EventHandler
    fun onInteractWithGameEntity(event: PlayerInteractAtEntityEvent) {
        if (BlockoGame.instance.gameEntityHandler.gameEntities.values().filter { it.livingEntity != null }.none { it.livingEntity!!.uniqueId == event.rightClicked.uniqueId }) return
        event.isCancelled = true
    }

    @EventHandler
    fun onDamageGameEntity(event: EntityDamageByEntityEvent) {
        if (BlockoGame.instance.gameEntityHandler.gameEntities.values().filter { it.livingEntity != null }.none { it.livingEntity!!.uniqueId == event.entity.uniqueId }) return
        event.isCancelled = true
    }

    @EventHandler
    fun onDamageInArenaWorld(event: EntityDamageEvent) {
        if (event.entity !is Player) return
        if (!shouldBeProtected(event.entity.world)) return
        event.isCancelled = true
    }

    @EventHandler
    fun onChickenAggDropInArenaWorld(event: EntityDropItemEvent) {
        if (!shouldBeProtected(event.entity.world)) return
        if (event.entity !is Chicken) return
        event.isCancelled = true
    }

    @EventHandler
    fun onFoodLevelChangeInArenaWorld(event: FoodLevelChangeEvent) {
        if (!shouldBeProtected(event.entity.world)) return
        event.isCancelled = true
    }

    @EventHandler
    fun onSwapItemInArenaWorld(event: PlayerSwapHandItemsEvent) {
        if (!shouldBeProtected(event.player.world)) return
        event.isCancelled = true
    }

    @EventHandler
    fun onBlockBreakInArenaWorld(event: BlockBreakEvent) {
        if (!shouldBeProtected(event.player.world)) return
        event.isCancelled = (event.player.gameMode != GameMode.CREATIVE)
    }

    @EventHandler
    fun onBlockPlaceInArenaWorld(event: BlockPlaceEvent) {
        if (!shouldBeProtected(event.player.world)) return
        event.isCancelled = (event.player.gameMode != GameMode.CREATIVE)
        event.setBuild(event.player.gameMode == GameMode.CREATIVE)
    }

    @EventHandler
    fun onItemDragInArenaWorld(event: InventoryDragEvent) {
        if (!shouldBeProtected(event.whoClicked.world)) return
        event.isCancelled = (event.whoClicked.gameMode != GameMode.CREATIVE)
    }

    @EventHandler
    fun onItemClickInArenaWorld(event: InventoryClickEvent) {
        if (!shouldBeProtected(event.whoClicked.world)) return
        event.isCancelled = (event.whoClicked.gameMode != GameMode.CREATIVE)
    }

    @EventHandler
    fun onItemDropInArenaWorld(event: PlayerDropItemEvent) {
        if (!shouldBeProtected(event.player.world)) return
        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onInteract(event: PlayerInteractEvent) {
        if (!shouldBeProtected(event.player.world)) return
        event.isCancelled = (event.player.gameMode != GameMode.CREATIVE)
    }

    @EventHandler
    fun onWeatherChangeInArenaWorld(event: WeatherChangeEvent) {
        if (!shouldBeProtected(event.world)) return
        event.isCancelled = true
    }

    @EventHandler
    fun onEntityCombust(event: EntityCombustEvent) {
        if (!shouldBeProtected(event.entity.world)) return
        event.isCancelled = true
    }

    private fun shouldBeProtected(world: World): Boolean {
        return isArenaWorld(world) || isLobbyWorld(world)
    }

    private fun isArenaWorld(world: World): Boolean {
        return this.plugin.gameArenaHandler.cachedArenas.any { it.gameWorld.name == world.name }
    }

    private fun isLobbyWorld(world: World): Boolean {
        val lobbySpawn = this.plugin.lobbySpawnHandler.lobbySpawn ?: return false
        return lobbySpawn.worldName == world.name
    }

}