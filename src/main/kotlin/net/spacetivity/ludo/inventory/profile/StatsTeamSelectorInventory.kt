package net.spacetivity.ludo.inventory.profile

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.inventory.api.annotation.InventoryProperties
import net.spacetivity.inventory.api.inventory.InventoryController
import net.spacetivity.inventory.api.inventory.InventoryProvider
import net.spacetivity.inventory.api.item.InteractiveItem
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.extensions.toStatsPlayerInstance
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.stats.StatsPlayer
import net.spacetivity.ludo.team.GameTeam
import net.spacetivity.ludo.translation.Translation
import net.spacetivity.ludo.utils.InventoryUtils
import net.spacetivity.ludo.utils.ItemBuilder
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag

@InventoryProperties(id = "stats_team_selector_inv", rows = 5, columns = 9)
class StatsTeamSelectorInventory(private val gameArena: GameArena) : InventoryProvider {

    override fun init(player: Player, controller: InventoryController) {
        val translation: Translation = LudoGame.instance.translationHandler.getSelectedTranslation()

        controller.fill(InventoryController.FillType.TOP_BORDER, InteractiveItem.placeholder(Material.BLACK_STAINED_GLASS_PANE))
        controller.fill(InventoryController.FillType.BOTTOM_BORDER, InteractiveItem.placeholder(Material.BLACK_STAINED_GLASS_PANE))

        controller.setItem(0, 4, InteractiveItem.of(ItemBuilder(Material.SLIME_BALL)
            .setName(translation.validateItemName("blocko.inventory_utils.back_item_display_name"))
            .build()) { _, _, _ ->
            val statsPlayer: StatsPlayer = LudoGame.instance.statsPlayerHandler.getStatsPlayer(player.uniqueId)
                ?: return@of
            InventoryUtils.openStatsInventory(player, statsPlayer)
        })

        val gameTeams: MutableCollection<GameTeam> = LudoGame.instance.gameTeamHandler.gameTeams[this.gameArena.id]

        for (column in 0..<4) {
            val gameTeam: GameTeam = gameTeams.find { it.teamId == column } ?: continue
            controller.setItem(2, column * 2 + 1, getTeamItem(player, gameTeam, translation))
        }
    }

    private fun getTeamItem(player: Player, gameTeam: GameTeam, translation: Translation): InteractiveItem {
        val teamColor: NamedTextColor = gameTeam.color

        val isNotEmptyTeam: Boolean = gameTeam.teamMembers.isNotEmpty()

        val teamDisplayNameKey = "blocko.inventory.stats_team_selector.team_item.display_name.${if (isNotEmptyTeam) "active" else "not_active"}"
        val teamLoreKey = "blocko.inventory.stats_team_selector.team_item.lore.${if (isNotEmptyTeam) "active" else "not_active"}"

        val itemBuilder = ItemBuilder(if (isNotEmptyTeam) Material.LEATHER_CHESTPLATE else Material.BARRIER)
            .setName(translation.validateItemName(teamDisplayNameKey,
                Placeholder.parsed("team_color", "<${gameTeam.color.asHexString()}>"),
                Placeholder.parsed("team_name", gameTeam.name)))
            .setLoreByComponent(translation.validateItemLore(teamLoreKey))
            .addFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ITEM_SPECIFICS, ItemFlag.HIDE_DYE)
            .setData("teamName", gameTeam.name)

        if (isNotEmptyTeam)
            itemBuilder.setArmorColor(Color.fromRGB(teamColor.red(), teamColor.green(), teamColor.blue()))

        return InteractiveItem.of(itemBuilder.build()) { _, _, _ ->

            if (!isNotEmptyTeam) return@of

            val gamePlayer: GamePlayer = this.gameArena.currentPlayers.find { it.uuid == gameTeam.teamMembers.first() }
                ?: return@of
            val statsPlayer: StatsPlayer = gamePlayer.toStatsPlayerInstance() ?: return@of

            InventoryUtils.openStatsInventory(player, statsPlayer)
        }
    }

}