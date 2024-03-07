package net.spacetivity.ludo.inventory

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.inventory.api.annotation.InventoryProperties
import net.spacetivity.inventory.api.inventory.InventoryController
import net.spacetivity.inventory.api.inventory.InventoryProvider
import net.spacetivity.inventory.api.item.InteractiveItem
import net.spacetivity.inventory.api.item.InventoryPosition
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.setup.GameArenaSetupData
import net.spacetivity.ludo.field.GameField
import net.spacetivity.ludo.utils.ItemBuilder
import net.spacetivity.ludo.field.PathFace
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player

@InventoryProperties(id = "turn_inv", rows = 1, columns = 9)
class GameFieldTurnSetupInventory(private val blockLocation: Location) : InventoryProvider {

    override fun init(player: Player, controller: InventoryController) {
        val availablePositions = listOf(
            InventoryPosition.of(0, 2),
            InventoryPosition.of(0, 3),
            InventoryPosition.of(0, 5),
            InventoryPosition.of(0, 6)
        )

        val items: List<InteractiveItem> = initItems(player)

        for (i in items.indices) {
            controller.setItem(availablePositions[i], items[i])
        }
    }

    private fun initItems(player: Player): List<InteractiveItem> {
        val items: MutableList<InteractiveItem> = mutableListOf()

        for (pathFace: PathFace in PathFace.entries) {
            items.add(InteractiveItem.of(ItemBuilder(Material.PLAYER_HEAD)
                .setName(Component.text(pathFace.name, NamedTextColor.BLUE))
                .setLoreByComponent(mutableListOf(Component.text("Click to set the field turn", NamedTextColor.YELLOW)))
                .setOwner(pathFace.headValue)
                .build())
            { _, _, _ ->
                player.closeInventory()

                val setupData: GameArenaSetupData = LudoGame.instance.gameArenaSetupHandler.getSetupData(player.uniqueId) ?: return@of
                val gameField: GameField = setupData.gameFields.find { it.x == this.blockLocation.x && it.z == this.blockLocation.z } ?: return@of
                LudoGame.instance.gameArenaSetupHandler.setTurn(player, gameField, this.blockLocation, pathFace)
            })
        }

        return items
    }

}