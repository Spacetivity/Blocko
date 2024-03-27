package net.spacetivity.blocko.listener

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.extensions.getArena
import net.spacetivity.blocko.lobby.LobbySpawn
import net.spacetivity.blocko.utils.ItemBuilder
import net.spacetivity.blocko.utils.PersistentDataUtils
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
import org.bukkit.inventory.ItemStack
import java.util.*

class ProtectionListener(private val plugin: BlockoGame) : Listener {

    init {
        this.plugin.server.pluginManager.registerEvents(this, this.plugin)
    }

    @EventHandler
    fun onServerListPing(event: ServerListPingEvent) {
        if (!this.plugin.globalConfigFile.motdEnabled) return
        event.motd(this.plugin.translationHandler.getSelectedTranslation().validateLine("blocko.motd"))
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
        event.isCancelled = true
    }

    @EventHandler
    fun onBlockPlaceInArenaWorld(event: BlockPlaceEvent) {
        if (!shouldBeProtected(event.player.world)) return
        event.isCancelled = true
        event.setBuild(false)
    }

    @EventHandler
    fun onItemDragInArenaWorld(event: InventoryDragEvent) {
        if (!shouldBeProtected(event.whoClicked.world)) return
        event.isCancelled = true
    }

    @EventHandler
    fun onItemClickInArenaWorld(event: InventoryClickEvent) {
        if (!shouldBeProtected(event.whoClicked.world)) return
        event.isCancelled = true
    }

    @EventHandler
    fun onItemDropInArenaWorld(event: PlayerDropItemEvent) {
        if (!shouldBeProtected(event.player.world)) return
        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onInteract(event: PlayerInteractEvent) {
        if (!shouldBeProtected(event.player.world)) return
        event.isCancelled = true
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
        val lobbySpawn: LobbySpawn = this.plugin.lobbySpawnHandler.lobbySpawn ?: return false
        return lobbySpawn.worldName == world.name
    }

    private fun shouldBeProtected(world: World, vararg player: Player): Boolean {
        val lobbySpawn: LobbySpawn? = this.plugin.lobbySpawnHandler.lobbySpawn
        return (lobbySpawn != null && lobbySpawn.worldName == world.name) || (player.size == 1 && (player[0].getArena() != null && player[0].getArena()!!.gameWorld.name == player[0].world.name))
    }

}