package net.spacetivity.blocko.inventory.profile

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.Arena
import net.spacetivity.blocko.item.*
import net.spacetivity.blocko.stats.toStatsPlayerInstance
import net.spacetivity.blocko.team.GameTeam
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.utils.Constants.TEAM_NAME_KEY
import net.spacetivity.blocko.utils.InventoryUtils
import net.spacetivity.inventory.api.inventory.InventoryController
import net.spacetivity.inventory.api.inventory.InventoryProperties
import net.spacetivity.inventory.api.inventory.InventoryProvider
import net.spacetivity.inventory.api.item.InteractiveItem
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.LeatherArmorMeta

@InventoryProperties(id = "stats_team_selector_inv", rows = 5, columns = 9)
class StatsTeamSelectorInventory(private val arena: Arena) : InventoryProvider {

    override fun init(player: Player, controller: InventoryController) {
        val translation = BlockoGame.instance.translationHandler.getSelectedTranslation()

        controller.fill(InventoryController.FillType.TOP_BORDER, InteractiveItem.placeholder(Material.BLACK_STAINED_GLASS_PANE))
        controller.fill(InventoryController.FillType.BOTTOM_BORDER, InteractiveItem.placeholder(Material.BLACK_STAINED_GLASS_PANE))

        controller.setItem(0, 4, InteractiveItem.of(itemStack(Material.SLIME_BALL) {
            meta {
                name = translation.displayName("blocko.inventory_utils.back_item_display_name")
            }
        }) { _, _, _ ->
            val statsPlayer = BlockoGame.instance.statsPlayerHandler.getStatsPlayer(player.uniqueId) ?: return@of
            InventoryUtils.openStatsInventory(player, statsPlayer)
        })

        val gameTeams = BlockoGame.instance.gameTeamHandler.gameTeams[this.arena.id]

        for (column in 0..<4) {
            val gameTeam = gameTeams.find { it.teamId == column } ?: continue
            controller.setItem(2, column * 2 + 1, getTeamItem(player, gameTeam, translation))
        }
    }

    private fun getTeamItem(player: Player, gameTeam: GameTeam, translation: Translation): InteractiveItem {
        val teamColor = gameTeam.color

        val isNotEmptyTeam = gameTeam.teamMembers.isNotEmpty()

        val teamDisplayNameKey = "blocko.inventory.stats_team_selector.team_item.display_name.${if (isNotEmptyTeam) "active" else "not_active"}"
        val teamLoreKey = "blocko.inventory.stats_team_selector.team_item.lore.${if (isNotEmptyTeam) "active" else "not_active"}"

        val teamItemStack = itemStack(if (isNotEmptyTeam) Material.LEATHER_CHESTPLATE else Material.BARRIER) {
            if (isNotEmptyTeam) {
                meta<LeatherArmorMeta> {
                    name = translation.displayName(teamDisplayNameKey,
                        Placeholder.parsed("team_color", "<${gameTeam.color.asHexString()}>"),
                        Placeholder.parsed("team_name", gameTeam.name))
                    lore(translation.lore(teamLoreKey))
                    hideExtraInfo()
                    setColor(Color.fromRGB(teamColor.red(), teamColor.green(), teamColor.blue()))
                    applyPersistentData(TEAM_NAME_KEY, gameTeam.name)
                }
            } else {
                meta {
                    name = translation.displayName(teamDisplayNameKey,
                        Placeholder.parsed("team_color", "<${gameTeam.color.asHexString()}>"),
                        Placeholder.parsed("team_name", gameTeam.name))
                    lore(translation.lore(teamLoreKey))
                    hideExtraInfo()
                    applyPersistentData(TEAM_NAME_KEY, gameTeam.name)
                }
            }
        }

        return InteractiveItem.of(teamItemStack) { _, _, _ ->
            if (!isNotEmptyTeam) return@of

            val gamePlayer = this.arena.currentPlayers.find { it.uuid == gameTeam.teamMembers.first() } ?: return@of
            val statsPlayer = gamePlayer.toStatsPlayerInstance() ?: return@of

            InventoryUtils.openStatsInventory(player, statsPlayer)
        }
    }

}