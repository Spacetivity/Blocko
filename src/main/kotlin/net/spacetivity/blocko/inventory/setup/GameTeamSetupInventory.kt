package net.spacetivity.blocko.inventory.setup

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.inventory.api.SpaceInventoryProvider
import net.spacetivity.inventory.api.annotation.InventoryProperties
import net.spacetivity.inventory.api.inventory.InventoryController
import net.spacetivity.inventory.api.inventory.InventoryProvider
import net.spacetivity.inventory.api.item.InteractiveItem
import net.spacetivity.inventory.api.item.InventoryPosition
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.setup.GameArenaSetupData
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.team.GameTeam
import net.spacetivity.blocko.utils.ItemBuilder
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
        val arenaSetupData: GameArenaSetupData = BlockoGame.instance.gameArenaSetupHandler.getSetupData(player.uniqueId)
            ?: return items

        for (gameTeam: GameTeam in arenaSetupData.gameTeams) {
            items.add(InteractiveItem.of(ItemBuilder(Material.LEATHER_CHESTPLATE)
                .setName(Component.text(gameTeam.name, gameTeam.color))
                .setLoreByComponent(mutableListOf(Component.text(if (this.type == InvType.GARAGE) "Click to set a garage field" else "Click to set a team entrance", NamedTextColor.YELLOW)))
                .setArmorColor(Color.fromRGB(gameTeam.color.red(), gameTeam.color.green(), gameTeam.color.blue()))
                .build())
            { _, _, _ ->
                player.closeInventory()
                when (this.type) {

                    InvType.GARAGE -> {
                        BlockoGame.instance.gameArenaSetupHandler.addGarageField(player, gameTeam.name, this.location)
                    }

                    InvType.IDS -> {
                        val setupData: GameArenaSetupData = BlockoGame.instance.gameArenaSetupHandler.getSetupData(player.uniqueId) ?: return@of
                        setupData.setupTool.currentTeamName = gameTeam.name
                        setupData.setupTool.fieldIndex = 0
                        player.sendMessage(Component.text("Now, please set the field ids for team ${gameTeam.name}!", NamedTextColor.GREEN))
                    }

                    else -> {
                        val possibleField: GameField? = arenaSetupData.gameFields.find { it.world == location.world && it.x == this.location.x && it.z == this.location.z }

                        if (possibleField == null) {
                            player.sendMessage(Component.text("Cannot set team entrance, field is not there!", NamedTextColor.RED))
                            return@of
                        }

                        possibleField.properties.teamEntrance = gameTeam.name

                        SpaceInventoryProvider.api.inventoryHandler.openStaticInventory(player, Component.text("Set a turn"), GameFieldTurnSetupInventory(location), true)
                    }

                }
            })
        }

        return items
    }

}

enum class InvType {
    GARAGE,
    ENTRANCE,
    IDS
}