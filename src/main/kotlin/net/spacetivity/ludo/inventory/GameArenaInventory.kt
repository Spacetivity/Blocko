package net.spacetivity.ludo.inventory

import net.spacetivity.inventory.api.annotation.InventoryProperties
import net.spacetivity.inventory.api.inventory.InventoryController
import net.spacetivity.inventory.api.inventory.InventoryProvider
import net.spacetivity.inventory.api.item.InteractiveItem
import net.spacetivity.inventory.api.item.InventoryPosition
import net.spacetivity.inventory.api.pagination.InventoryPagination
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.utils.HeadUtils
import net.spacetivity.ludo.utils.ItemUtils
import org.bukkit.Material
import org.bukkit.entity.Player

@InventoryProperties(id = "arena_inv", rows = 6, columns = 9)
class GameArenaInventory : InventoryProvider {

    override fun init(player: Player, controller: InventoryController) {
        val pagination: InventoryPagination = controller.createPagination()
        pagination.limitItemsPerPage(32)
        pagination.setItemField(0, 0, 3, 8)

        val arenaItems: MutableList<InteractiveItem> = mutableListOf()

        for (gameArena: GameArena in LudoGame.instance.gameArenaHandler.cachedArenas) {
            val hostName: String = if (gameArena.arenaHost == null) "-/-" else gameArena.arenaHost!!.name

            val arenaItem = InteractiveItem.of(
                ItemUtils(Material.MAP).setName(gameArena.id).setLoreByString(
                    mutableListOf(
                        "",
                        "Status ${gameArena.status.name}",
                        "Phase ${gameArena.phase.name}",
                        "Host: $hostName",
                        "Playing: ${gameArena.currentPlayers}/${gameArena.maxPlayers}",
                        "World: ${gameArena.gameWorld.name}",
                        " ",
                        "Click for more options..."
                    )
                ).build()
            )

            arenaItems.add(arenaItem)
        }

        pagination.distributeItems(arenaItems)

        controller.fill(InventoryController.FillType.ROW, InteractiveItem.placeholder(Material.LIME_STAINED_GLASS_PANE), InventoryPosition.of(4, 0))

        controller.setItem(5, 0, InteractiveItem.nextPage(ItemUtils(Material.ARROW)
            .setName(">>")
            .build(), pagination))

        controller.setItem(5, 4, InteractiveItem.of(ItemUtils(Material.PLAYER_HEAD)
            .setName("Create Arena")
            .setOwner(HeadUtils.PLUS)
            .build()) { _, _, _ -> player.performCommand("ludo arena init") })

        controller.setItem(5, 8, InteractiveItem.nextPage(ItemUtils(Material.SPECTRAL_ARROW).setName("<<").build(), pagination))
    }

}