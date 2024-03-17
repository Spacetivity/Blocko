package net.spacetivity.ludo.inventory.profile

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.inventory.api.annotation.InventoryProperties
import net.spacetivity.inventory.api.inventory.InventoryController
import net.spacetivity.inventory.api.inventory.InventoryProvider
import net.spacetivity.inventory.api.item.InteractiveItem
import net.spacetivity.inventory.api.item.InventoryPosition
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.extensions.getArena
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.stats.StatsPlayer
import net.spacetivity.ludo.stats.StatsType
import net.spacetivity.ludo.translation.Translation
import net.spacetivity.ludo.utils.HeadUtils
import net.spacetivity.ludo.utils.InventoryUtils
import net.spacetivity.ludo.utils.ItemBuilder
import org.bukkit.Material
import org.bukkit.entity.Player

@InventoryProperties(id = "stats_inv", rows = 5, columns = 9)
class StatsInventory(private val gameArena: GameArena, private val statsPlayer: StatsPlayer, private val showSearchPlayerItem: Boolean) : InventoryProvider {

    override fun init(player: Player, controller: InventoryController) {
        val translation: Translation = LudoGame.instance.translationHandler.getSelectedTranslation()

        controller.fill(InventoryController.FillType.TOP_BORDER, InteractiveItem.placeholder(Material.BLACK_STAINED_GLASS_PANE))
        controller.fill(InventoryController.FillType.BOTTOM_BORDER, InteractiveItem.placeholder(Material.BLACK_STAINED_GLASS_PANE))

        controller.setItem(0, 4, InteractiveItem.of(ItemBuilder(Material.SLIME_BALL)
            .setName(translation.validateItemName("blocko.inventory_utils.back_item_display_name"))
            .build()) { _, _, _ ->

            if (this.statsPlayer.uuid != player.uniqueId) {
                val selfStatsPlayer: StatsPlayer = LudoGame.instance.statsPlayerHandler.getStatsPlayer(player.uniqueId) ?: return@of
                InventoryUtils.openStatsInventory(player, selfStatsPlayer)
                return@of
            }

            val gameArena: GameArena = player.getArena() ?: return@of
            val isShopItemActive: Boolean = gameArena.phase.isIdle()
            InventoryUtils.openProfileInventory(player, isShopItemActive)
        })

        for (column in 0..<4) {
            val statsType: StatsType = StatsType.entries[column]
            controller.setItem(2, column * 2 + 1, getStatsItem(translation, statsType))
        }

        val gamePlayer: GamePlayer = this.gameArena.currentPlayers.find { it.uuid == this.statsPlayer.uuid } ?: return

        controller.setItem(InventoryPosition.of(4, if (this.showSearchPlayerItem) 2 else 4), InteractiveItem.of(ItemBuilder(Material.PLAYER_HEAD)
            .setName(translation.validateItemName("blocko.inventory.stats.overview_item.display_name"))
            .setLoreByComponent(translation.validateItemLore("blocko.inventory.stats.overview_item.lore",
                Placeholder.parsed("eliminations_key", translation.validateLineAsString("blocko.stats.type.eliminations")),
                Placeholder.parsed("eliminations_value", this.statsPlayer.eliminatedOpponents.toString()),

                Placeholder.parsed("knocked_out_by_opponents_key", translation.validateLineAsString("blocko.stats.type.knocked_out_by_opponents")),
                Placeholder.parsed("knocked_out_by_opponents_value", this.statsPlayer.knockedOutByOpponents.toString()),

                Placeholder.parsed("coins_key", translation.validateLineAsString("blocko.stats.type.coins")),
                Placeholder.parsed("coins_value", this.statsPlayer.coins.toString()),

                Placeholder.parsed("played_games_key", translation.validateLineAsString("blocko.stats.type.played_games")),
                Placeholder.parsed("played_games_value", this.statsPlayer.playedGames.toString())))
            .setOwner(if (gamePlayer.isAI) HeadUtils.BOT else player.playerProfile.properties.first().value)
            .build()))

        if (this.showSearchPlayerItem) {
            controller.setItem(4, 6, InteractiveItem.of(ItemBuilder(Material.NAME_TAG)
                .setName(translation.validateItemName("blocko.inventory.stats.search_player_item.display_name"))
                .setLoreByComponent(translation.validateItemLore("blocko.inventory.stats.search_player_item.lore"))
                .build()) { _, _, _ -> InventoryUtils.openStatsTeamSelectorInventory(player) })
        }
    }

    private fun getStatsItem(translation: Translation, statsType: StatsType): InteractiveItem {
        return InteractiveItem.of(ItemBuilder(Material.PAPER)
            .setName(translation.validateItemName("blocko.inventory.stats.stats_type_item.display_name",
                Placeholder.parsed("type_name", translation.validateLineAsString(statsType.nameKey)),
                Placeholder.parsed("value", this.statsPlayer.getStatsValue(statsType).toString())))
            .build())
    }

}