package net.spacetivity.blocko.inventory.setup

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.setup.GameArenaSetupData
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.item.hideExtraInfo
import net.spacetivity.blocko.item.itemStack
import net.spacetivity.blocko.item.meta
import net.spacetivity.blocko.item.name
import net.spacetivity.blocko.team.GameTeam
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.translation.translateMessage
import net.spacetivity.blocko.utils.InventoryUtils
import net.spacetivity.inventory.api.inventory.InventoryController
import net.spacetivity.inventory.api.inventory.InventoryProperties
import net.spacetivity.inventory.api.inventory.InventoryProvider
import net.spacetivity.inventory.api.item.InteractiveItem
import net.spacetivity.inventory.api.item.InventoryPos
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.LeatherArmorMeta

@InventoryProperties(id = "garage_field_inv", rows = 1, 9)
class GameTeamSetupInventory(private val type: InvType, private val location: Location) : InventoryProvider {

    override fun init(player: Player, controller: InventoryController) {
        val translation: Translation = BlockoGame.instance.translationHandler.getSelectedTranslation()

        val availablePositions: List<InventoryPos> = listOf(
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
            items.add(InteractiveItem.of(itemStack(Material.LEATHER_CHESTPLATE) {
                meta<LeatherArmorMeta> {
                    name = translation.displayName("blocko.inventory.game_team_setup.team_item.display_name",
                        Placeholder.parsed("team_color", "<${gameTeam.color.asHexString()}>"),
                        Placeholder.parsed("team_name", gameTeam.name.lowercase().replaceFirstChar { it.uppercase() }))

                    lore(translation.lore("blocko.inventory.game_team_setup.team_item.lore.entrance"))
                    setColor(Color.fromRGB(gameTeam.color.red(), gameTeam.color.green(), gameTeam.color.blue()))
                    hideExtraInfo()
                }
            })
            { _, _, _ ->
                player.closeInventory()
                when (this.type) {
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
    IDS,
    ENTRANCE
}