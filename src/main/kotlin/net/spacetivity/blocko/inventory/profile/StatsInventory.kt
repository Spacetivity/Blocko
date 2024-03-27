package net.spacetivity.blocko.inventory.profile

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.extensions.getArena
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.stats.StatsPlayer
import net.spacetivity.blocko.stats.StatsType
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.utils.HeadUtils
import net.spacetivity.blocko.utils.InventoryUtils
import net.spacetivity.blocko.utils.ItemBuilder
import net.spacetivity.blocko.utils.NumberUtils
import net.spacetivity.inventory.api.annotation.InventoryProperties
import net.spacetivity.inventory.api.inventory.InventoryController
import net.spacetivity.inventory.api.inventory.InventoryProvider
import net.spacetivity.inventory.api.item.InteractiveItem
import net.spacetivity.inventory.api.item.InventoryPosition
import org.bukkit.Material
import org.bukkit.entity.Player

@InventoryProperties(id = "stats_inv", rows = 5, columns = 9)
class StatsInventory(private val gameArena: GameArena, private val statsPlayer: StatsPlayer, private val showSearchPlayerItem: Boolean) : InventoryProvider {

    override fun init(player: Player, controller: InventoryController) {
        val translation: Translation = BlockoGame.instance.translationHandler.getSelectedTranslation()

        controller.fill(InventoryController.FillType.TOP_BORDER, InteractiveItem.placeholder(Material.BLACK_STAINED_GLASS_PANE))
        controller.fill(InventoryController.FillType.BOTTOM_BORDER, InteractiveItem.placeholder(Material.BLACK_STAINED_GLASS_PANE))

        controller.setItem(0, 4, InteractiveItem.of(ItemBuilder(Material.SLIME_BALL)
            .setName(translation.validateItemName("blocko.inventory_utils.back_item_display_name"))
            .build()) { _, _, _ ->

            if (this.statsPlayer.uuid != player.uniqueId) {
                val selfStatsPlayer: StatsPlayer = BlockoGame.instance.statsPlayerHandler.getStatsPlayer(player.uniqueId)
                    ?: return@of
                InventoryUtils.openStatsInventory(player, selfStatsPlayer)
                return@of
            }

            val gameArena: GameArena = player.getArena() ?: return@of
            val isShopItemActive: Boolean = gameArena.phase.isIdle()
            InventoryUtils.openProfileInventory(player, isShopItemActive)
        })

        val gamePlayer: GamePlayer = this.gameArena.currentPlayers.find { it.uuid == this.statsPlayer.uuid } ?: return

        for (column in 0..<4) {
            val statsType: StatsType = StatsType.entries[column]
            controller.setItem(2, column * 2 + 1, getStatsItem(translation, gamePlayer.isAI, statsType))
        }

        val skinValue: String =
            if (this.statsPlayer.uuid == player.uniqueId)
                player.playerProfile.properties.first().value
            else
                gamePlayer.toBukkitInstance()?.playerProfile?.properties?.first()?.value ?: HeadUtils.BOT

        controller.setItem(InventoryPosition.of(4, if (this.showSearchPlayerItem) 2 else 4), InteractiveItem.of(ItemBuilder(Material.PLAYER_HEAD)
            .setName(translation.validateItemName("blocko.inventory.stats.overview_item.display_name"))
            .setLoreByComponent(translation.validateItemLore("blocko.inventory.stats.overview_item.lore",
                Placeholder.parsed("eliminations_key", translation.validateLineAsString("blocko.stats.type.eliminations")),
                Placeholder.parsed("eliminations_value", this.statsPlayer.eliminatedOpponents.toString()),

                Placeholder.parsed("knocked_out_by_opponents_key", translation.validateLineAsString("blocko.stats.type.knocked_out_by_opponents")),
                Placeholder.parsed("knocked_out_by_opponents_value", this.statsPlayer.knockedOutByOpponents.toString()),

                Placeholder.parsed("coins_key", translation.validateLineAsString("blocko.stats.type.coins")),
                Placeholder.parsed("coins_value", NumberUtils.format(this.statsPlayer.coins)),

                Placeholder.parsed("played_games_key", translation.validateLineAsString("blocko.stats.type.played_games")),
                Placeholder.parsed("played_games_value", this.statsPlayer.playedGames.toString()),

                Placeholder.parsed("won_games_key", translation.validateLineAsString("blocko.stats.type.won_games")),
                Placeholder.parsed("won_games_value", this.statsPlayer.wonGames.toString())))
            .setOwner(if (gamePlayer.isAI) HeadUtils.BOT else skinValue)
            .build()))

        if (this.showSearchPlayerItem) {
            controller.setItem(4, 6, InteractiveItem.of(ItemBuilder(Material.NAME_TAG)
                .setName(translation.validateItemName("blocko.inventory.stats.search_player_item.display_name"))
                .setLoreByComponent(translation.validateItemLore("blocko.inventory.stats.search_player_item.lore"))
                .build()) { _, _, _ -> InventoryUtils.openStatsTeamSelectorInventory(player) })
        }
    }

    private fun getStatsItem(translation: Translation, isAI: Boolean, statsType: StatsType): InteractiveItem {
        val statsValue: Int = this.statsPlayer.getStatsValue(statsType)

        val displayAsAI: Boolean = isAI && (statsType == StatsType.COINS || statsType == StatsType.PLAYED_GAMES)
        val displayNameKey = "blocko.inventory.stats.stats_type_item.display_name.${if (displayAsAI) "not_active" else "active"}"

        return InteractiveItem.of(ItemBuilder(if (displayAsAI) Material.BARRIER else Material.PAPER)
            .setName(translation.validateItemName(displayNameKey,
                Placeholder.parsed("type_name", translation.validateLineAsString(statsType.nameKey)),
                Placeholder.parsed("value", if (statsType == StatsType.COINS) NumberUtils.format(statsValue) else statsValue.toString())))
            .build())
    }

}