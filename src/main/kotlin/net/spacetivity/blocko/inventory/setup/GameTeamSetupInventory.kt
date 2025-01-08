package net.spacetivity.blocko.inventory.setup

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.setup.GameArenaSetupData
import net.spacetivity.blocko.extensions.translateMessage
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.team.GameTeam
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.utils.InventoryUtils
import net.spacetivity.blocko.utils.ItemBuilder
import net.spacetivity.inventory.api.inventory.InventoryController
import net.spacetivity.inventory.api.inventory.InventoryProperties
import net.spacetivity.inventory.api.inventory.InventoryProvider
import net.spacetivity.inventory.api.item.InteractiveItem
import net.spacetivity.inventory.api.item.InventoryPos
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player

@InventoryProperties(id = "garage_field_inv", rows = 1, 9)
class GameTeamSetupInventory(private val type: InvType, private val location: Location) : InventoryProvider {

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
        val arenaSetupData: GameArenaSetupData = BlockoGame.instance.gameArenaSetupHandler.getSetupData(player.uniqueId)
            ?: return items

        for (gameTeam: GameTeam in arenaSetupData.gameTeams) {
            items.add(InteractiveItem.of(ItemBuilder(Material.LEATHER_CHESTPLATE)
                .setName(translation.validateItemName("blocko.inventory.game_team_setup.team_item.display_name",
                    Placeholder.parsed("team_color", "<${gameTeam.color.asHexString()}>"),
                    Placeholder.parsed("team_name", gameTeam.name.lowercase().replaceFirstChar { it.uppercase() })))
                .setLoreByComponent(translation.validateItemLore("blocko.inventory.game_team_setup.team_item.lore.${if (this.type == InvType.GARAGE) "garage" else "entrance"}"))
                .setArmorColor(Color.fromRGB(gameTeam.color.red(), gameTeam.color.green(), gameTeam.color.blue()))
                .build())
            { _, _, _ ->
                player.closeInventory()
                when (this.type) {

                    InvType.GARAGE -> {
                        BlockoGame.instance.gameArenaSetupHandler.addGarageField(player, gameTeam.name, this.location)
                    }

                    InvType.IDS -> {
                        val setupData: GameArenaSetupData = BlockoGame.instance.gameArenaSetupHandler.getSetupData(player.uniqueId)
                            ?: return@of
                        setupData.setupTool.currentTeamName = gameTeam.name
                        setupData.setupTool.fieldIndex = 0
                        player.translateMessage("blocko.inventory.game_team_setup.team_item.click.set_field_ids",
                            Placeholder.parsed("team_color", "<${gameTeam.color.asHexString()}>"),
                            Placeholder.parsed("team_name", gameTeam.name.lowercase().replaceFirstChar { it.uppercase() }))
                    }

                    else -> {
                        val possibleField: GameField? = arenaSetupData.gameFields.find { it.world == location.world && it.x == this.location.x && it.z == this.location.z }

                        if (possibleField == null) {
                            player.translateMessage("blocko.inventory.game_team_setup.team_item.click.cannot_set_team_entrance")
                            return@of
                        }

                        possibleField.properties.teamEntrance = gameTeam.name
                        InventoryUtils.openGameFieldTurnInventory(player, location)
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