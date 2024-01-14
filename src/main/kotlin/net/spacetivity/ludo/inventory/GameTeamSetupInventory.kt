package net.spacetivity.ludo.inventory

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.inventory.api.SpaceInventoryProvider
import net.spacetivity.inventory.api.annotation.InventoryProperties
import net.spacetivity.inventory.api.inventory.InventoryController
import net.spacetivity.inventory.api.inventory.InventoryProvider
import net.spacetivity.inventory.api.item.InteractiveItem
import net.spacetivity.inventory.api.item.InventoryPosition
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.setup.GameArenaSetupData
import net.spacetivity.ludo.field.GameField
import net.spacetivity.ludo.team.GameTeam
import net.spacetivity.ludo.utils.ItemUtils
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player

@InventoryProperties(id = "garage_field_inv", rows = 1, 9)
class GameTeamSetupInventory(private val type: InvType, private val location: Location) : InventoryProvider {

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
        val arenaSetupData: GameArenaSetupData = LudoGame.instance.gameArenaSetupHandler.getSetupData(player.uniqueId)
            ?: return items

        for (gameTeam: GameTeam in arenaSetupData.gameTeams) {
            items.add(InteractiveItem.of(ItemUtils(Material.LEATHER_CHESTPLATE)
                .setName(Component.text(gameTeam.name, gameTeam.color))
                .setLoreByComponent(mutableListOf(Component.text(if (this.type == InvType.GARAGE) "Click to set a garage field" else "Click to set a team entrance", NamedTextColor.YELLOW)))
                .setArmorColor(Color.fromRGB(gameTeam.color.red(), gameTeam.color.green(), gameTeam.color.blue()))
                .build())
            { _, _, _ ->
                player.closeInventory()
                if (this.type == InvType.GARAGE) {

                    if (arenaSetupData.gameGarageFields.filter { it.teamName == gameTeam.name }.toList().size >= 4) {
                        player.sendMessage(Component.text("You cannot set more then 4 team garage fields!", NamedTextColor.RED))
                        return@of
                    }

                    LudoGame.instance.gameArenaSetupHandler.addGarageField(player, gameTeam.name, this.location)
                } else {
                    LudoGame.instance.gameArenaSetupHandler.setTeamEntrance(player, gameTeam.name, this.location)

                    val gameField: GameField = arenaSetupData.gameFields.find { it.x == this.location.x && it.z == this.location.z }
                        ?: return@of

                    SpaceInventoryProvider.api.inventoryHandler.openStaticInventory(player, Component.text("Set a turn"), GameFieldTurnSetupInventory(gameField, location), true)
                }
            })
        }

        return items
    }

}

enum class InvType {
    GARAGE,
    ENTRANCE
}