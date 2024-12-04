package net.spacetivity.blocko.inventory.setup

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.setup.GameArenaSetupData
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.field.PathFace
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.utils.ItemBuilder
import net.spacetivity.inventory.api.inventory.InventoryController
import net.spacetivity.inventory.api.inventory.InventoryProperties
import net.spacetivity.inventory.api.inventory.InventoryProvider
import net.spacetivity.inventory.api.item.InteractiveItem
import net.spacetivity.inventory.api.item.InventoryPos
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player

@InventoryProperties(id = "turn_inv", rows = 1, columns = 9)
class GameFieldTurnSetupInventory(private val blockLocation: Location) : InventoryProvider {

    override fun init(player: Player, controller: InventoryController) {
        val translation: Translation = BlockoGame.instance.translationHandler.getSelectedTranslation()

        val availablePositions = listOf(
            InventoryPos.of(0, 2),
            InventoryPos.of(0, 3),
            InventoryPos.of(0, 5),
            InventoryPos.of(0, 6)
        )

        val items: List<InteractiveItem> = initItems(translation, player)

        for (i in items.indices) {
            controller.setItem(availablePositions[i], items[i])
        }
    }

    private fun initItems(translation: Translation, player: Player): List<InteractiveItem> {
        val items: MutableList<InteractiveItem> = mutableListOf()

        for (pathFace: PathFace in PathFace.entries) {
            items.add(InteractiveItem.of(ItemBuilder(Material.PLAYER_HEAD)
                .setName(translation.validateItemName("blocko.inventory.game_field_set_turn.turn_item.display_name", Placeholder.parsed("face", pathFace.name)))
                .setLoreByComponent(translation.validateItemLore("blocko.inventory.game_field_set_turn.turn_item.lore"))
                .setOwner(pathFace.headValue)
                .build())
            { _, _, _ ->
                player.closeInventory()
                val setupData: GameArenaSetupData = BlockoGame.instance.gameArenaSetupHandler.getSetupData(player.uniqueId)
                    ?: return@of
                val gameField: GameField = setupData.gameFields.find { it.x == this.blockLocation.x && it.z == this.blockLocation.z }
                    ?: return@of
                BlockoGame.instance.gameArenaSetupHandler.setTurn(player, gameField, this.blockLocation, pathFace)
            })
        }

        return items
    }

}