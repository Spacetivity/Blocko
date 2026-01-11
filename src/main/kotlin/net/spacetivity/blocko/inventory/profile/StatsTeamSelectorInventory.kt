package net.spacetivity.blocko.inventory.profile

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.arena.Arena
import net.spacetivity.blocko.item.*
import net.spacetivity.blocko.stats.toStatsPlayerInstance
import net.spacetivity.blocko.team.GameTeam
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.utils.Constants.TEAM_NAME_KEY
import net.spacetivity.blocko.utils.InventoryUtils
import net.spacetivity.inventorylib.api.GuiProvider
import net.spacetivity.inventorylib.api.inventory.GuiController
import net.spacetivity.inventorylib.api.inventory.GuiProperties
import net.spacetivity.inventorylib.api.inventory.Gui
import net.spacetivity.inventorylib.api.item.GuiItem
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.LeatherArmorMeta

@GuiProperties(id = "stats_team_selector_inv", rows = 5, columns = 9)
class StatsTeamSelectorInventory(private val arena: Arena) : Gui {

    override fun init(player: Player, controller: GuiController) {
        val translation = Blocko.instance.translationHandler.getSelectedTranslation()

        controller.fill(GuiController.FillType.TOP_BORDER, GuiProvider.api.placeholder(Material.BLACK_STAINED_GLASS_PANE))
        controller.fill(GuiController.FillType.BOTTOM_BORDER, GuiProvider.api.placeholder(Material.BLACK_STAINED_GLASS_PANE))

        controller.setItem(0, 4, GuiProvider.api.of(itemStack(Material.SLIME_BALL) {
            meta {
                name = translation.displayName("blocko.inventory_utils.back_item_display_name")
            }
        }) { _, _, _ ->
            val statsPlayer = Blocko.instance.statsPlayerHandler.getStatsPlayer(player.uniqueId) ?: return@of
            InventoryUtils.openStatsInventory(player, statsPlayer)
        })

        val gameTeams = Blocko.instance.gameTeamHandler.gameTeams[this.arena.id]

        for (column in 0..<4) {
            val gameTeam = gameTeams.find { it.teamId == column } ?: continue
            controller.setItem(2, column * 2 + 1, getTeamItem(player, gameTeam, translation))
        }
    }

    private fun getTeamItem(player: Player, gameTeam: GameTeam, translation: Translation): GuiItem {
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

        return GuiProvider.api.of(teamItemStack) { _, _, _ ->
            if (!isNotEmptyTeam) return@of

            val gamePlayer = this.arena.currentPlayers.find { it.uuid == gameTeam.teamMembers.first() } ?: return@of
            val statsPlayer = gamePlayer.toStatsPlayerInstance() ?: return@of

            InventoryUtils.openStatsInventory(player, statsPlayer)
        }
    }

}