package net.spacetivity.blocko.inventory.profile

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.arena.getArena
import net.spacetivity.blocko.item.itemStack
import net.spacetivity.blocko.item.meta
import net.spacetivity.blocko.item.name
import net.spacetivity.blocko.item.setValue
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.stats.StatsPlayer
import net.spacetivity.blocko.stats.StatsType
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.utils.Constants
import net.spacetivity.blocko.utils.InventoryUtils
import net.spacetivity.blocko.utils.NumberUtils
import net.spacetivity.inventory.api.inventory.InventoryController
import net.spacetivity.inventory.api.inventory.InventoryProperties
import net.spacetivity.inventory.api.inventory.InventoryProvider
import net.spacetivity.inventory.api.item.InteractiveItem
import net.spacetivity.inventory.api.item.InventoryPos
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.SkullMeta

@InventoryProperties(id = "stats_inv", rows = 5, columns = 9)
class StatsInventory(private val gameArena: GameArena, private val statsPlayer: StatsPlayer, private val showSearchPlayerItem: Boolean) : InventoryProvider {

    override fun init(player: Player, controller: InventoryController) {
        val translation: Translation = BlockoGame.instance.translationHandler.getSelectedTranslation()

        controller.fill(InventoryController.FillType.TOP_BORDER, InteractiveItem.placeholder(Material.BLACK_STAINED_GLASS_PANE))
        controller.fill(InventoryController.FillType.BOTTOM_BORDER, InteractiveItem.placeholder(Material.BLACK_STAINED_GLASS_PANE))

        controller.setItem(0, 4, InteractiveItem.of(itemStack(Material.SLIME_BALL) {
            meta {
                name = translation.displayName("blocko.inventory_utils.back_item_display_name")
            }
        }) { _, _, _ ->

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
                gamePlayer.toBukkitInstance()?.playerProfile?.properties?.first()?.value ?: Constants.BOT_SKULL

        controller.setItem(InventoryPos.of(4, if (this.showSearchPlayerItem) 2 else 4), InteractiveItem.of(itemStack(Material.PLAYER_HEAD) {
            meta<SkullMeta> {
                name = translation.displayName("blocko.inventory.stats.overview_item.display_name")
                lore(translation.lore("blocko.inventory.stats.overview_item.lore",
                    Placeholder.parsed("eliminations_key", translation.lineAsString("blocko.stats.type.eliminations")),
                    Placeholder.parsed("eliminations_value", statsPlayer.eliminatedOpponents.toString()),

                    Placeholder.parsed("knocked_out_by_opponents_key", translation.lineAsString("blocko.stats.type.knocked_out_by_opponents")),
                    Placeholder.parsed("knocked_out_by_opponents_value", statsPlayer.knockedOutByOpponents.toString()),

                    Placeholder.parsed("coins_key", translation.lineAsString("blocko.stats.type.coins")),
                    Placeholder.parsed("coins_value", NumberUtils.format(statsPlayer.coins)),

                    Placeholder.parsed("played_games_key", translation.lineAsString("blocko.stats.type.played_games")),
                    Placeholder.parsed("played_games_value", statsPlayer.playedGames.toString()),

                    Placeholder.parsed("won_games_key", translation.lineAsString("blocko.stats.type.won_games")),
                    Placeholder.parsed("won_games_value", statsPlayer.wonGames.toString())))

                setValue(if (gamePlayer.isAI) Constants.BOT_SKULL else skinValue)
            }
        }))

        if (this.showSearchPlayerItem) {
            controller.setItem(4, 6, InteractiveItem.of(itemStack(Material.NAME_TAG) {
                meta {
                    name = translation.displayName("blocko.inventory.stats.search_player_item.display_name")
                    lore(translation.lore("blocko.inventory.stats.search_player_item.lore"))
                }
            }) { _, _, _ -> InventoryUtils.openStatsTeamSelectorInventory(player) })
        }
    }

    private fun getStatsItem(translation: Translation, isAI: Boolean, statsType: StatsType): InteractiveItem {
        val statsValue: Int = this.statsPlayer.getStatsValue(statsType)

        val displayAsAI: Boolean = isAI && (statsType == StatsType.COINS || statsType == StatsType.PLAYED_GAMES)
        val displayNameKey = "blocko.inventory.stats.stats_type_item.display_name.${if (displayAsAI) "not_active" else "active"}"

        return InteractiveItem.of(itemStack(if (displayAsAI) Material.BARRIER else Material.PAPER) {
            meta {
                name = translation.displayName(displayNameKey,
                    Placeholder.parsed("type_name", translation.lineAsString(statsType.nameKey)),
                    Placeholder.parsed("value", if (statsType == StatsType.COINS) NumberUtils.format(statsValue) else statsValue.toString()))
            }
        })
    }

}