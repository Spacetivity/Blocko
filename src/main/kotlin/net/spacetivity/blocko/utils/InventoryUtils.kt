package net.spacetivity.blocko.utils

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.arena.getArena
import net.spacetivity.blocko.inventory.host.HostSettingsInventory
import net.spacetivity.blocko.inventory.host.InvitationInventory
import net.spacetivity.blocko.inventory.profile.*
import net.spacetivity.blocko.inventory.setup.GameFieldTurnSetupInventory
import net.spacetivity.blocko.inventory.setup.GameTeamSetupInventory
import net.spacetivity.blocko.inventory.setup.InvType
import net.spacetivity.blocko.inventory.team.TeamSelectorInventory
import net.spacetivity.blocko.item.itemStack
import net.spacetivity.blocko.item.meta
import net.spacetivity.blocko.item.name
import net.spacetivity.blocko.stats.StatsPlayer
import net.spacetivity.inventory.api.extension.openStaticInventory
import net.spacetivity.inventory.api.inventory.InventoryController
import net.spacetivity.inventory.api.item.InteractiveItem
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player

object InventoryUtils {

    fun openGameFieldTurnInventory(opener: Player, location: Location) {
        val title = BlockoGame.instance.translationHandler.getSelectedTranslation().line("blocko.inventory.game_field_set_turn.title")
        openStaticInventory(opener, title, GameFieldTurnSetupInventory(location))
    }

    fun openGameTeamSetupInventory(opener: Player, invType: InvType, block: Block) {
        val title = BlockoGame.instance.translationHandler.getSelectedTranslation().line("blocko.inventory.game_team_setup.title.entrance")
        openStaticInventory(opener, title, GameTeamSetupInventory(invType, block.location))
    }

    fun openHostSettingsInventory(opener: Player, gameArena: GameArena) {
        val title = BlockoGame.instance.translationHandler.getSelectedTranslation().line("blocko.inventory.host.title")
        openStaticInventory(opener, title, HostSettingsInventory(gameArena))
    }

    fun openInvitationInventory(opener: Player, gameArena: GameArena) {
        val title = BlockoGame.instance.translationHandler.getSelectedTranslation().line("blocko.inventory.invitation.title")
        openStaticInventory(opener, title, InvitationInventory(gameArena))
    }

    fun openTeamSelectorInventory(opener: Player, gameArena: GameArena) {
        val title = BlockoGame.instance.translationHandler.getSelectedTranslation().line("blocko.inventory.team_selector.title")
        openStaticInventory(opener, title, TeamSelectorInventory(gameArena))
    }

    fun openProfileInventory(opener: Player, isShopItemActive: Boolean) {
        val title = BlockoGame.instance.translationHandler.getSelectedTranslation().line("blocko.inventory.profile.title")
        openStaticInventory(opener, title, ProfileInventory(isShopItemActive))
    }

    fun openStatsTeamSelectorInventory(opener: Player) {
        val title = BlockoGame.instance.translationHandler.getSelectedTranslation().line("blocko.inventory.stats_team_selector.title")
        val gameArena = opener.getArena() ?: return
        openStaticInventory(opener, title, StatsTeamSelectorInventory(gameArena))
    }

    fun openStatsInventory(opener: Player, statsPlayer: StatsPlayer) {
        val translation = BlockoGame.instance.translationHandler.getSelectedTranslation()

        val gameArena = opener.getArena() ?: return
        val gamePlayer = gameArena.currentPlayers.find { it.uuid == statsPlayer.uuid } ?: return

        val botIndicatorString = translation.lineAsString("blocko.bot.indicator")
        val suffixPlaceholder = if (gamePlayer.isAI) Placeholder.parsed("suffix", botIndicatorString) else Placeholder.parsed("suffix", "")

        val titleKey = "blocko.inventory.stats.title.${if (opener.uniqueId == statsPlayer.uuid) "matching_name" else "other_name"}"
        val title = translation.line(titleKey, Placeholder.parsed("player_name", gamePlayer.name), suffixPlaceholder)

        val showSearchPlayerItem: Boolean = gameArena.phase.isIngame()
        openStaticInventory(opener, title, StatsInventory(gameArena, statsPlayer, showSearchPlayerItem))
    }

    fun openAchievementsInventory(opener: Player) {
        val title = BlockoGame.instance.translationHandler.getSelectedTranslation().line("blocko.inventory.achievements.title")
        openStaticInventory(opener, title, AchievementsInventory())
    }

    fun openEntityShopInventory(opener: Player) {
        val title = BlockoGame.instance.translationHandler.getSelectedTranslation().line("blocko.inventory.entity_shop.title")
        openStaticInventory(opener, title, EntityShopInventory())
    }

    fun setPreviousPageItem(row: Int, column: Int, controller: InventoryController) {
        val translation = BlockoGame.instance.translationHandler.getSelectedTranslation()
        val pagination = controller.pagination ?: return

        controller.setItem(row, column, InteractiveItem.previousPage(itemStack(Material.ARROW) {
            meta {
                name = translation.displayName("blocko.inventory_utils.previous_page_item_display_name")
            }
        }, pagination))
    }

    fun setNextPageItem(row: Int, column: Int, controller: InventoryController) {
        val translation = BlockoGame.instance.translationHandler.getSelectedTranslation()
        val pagination = controller.pagination ?: return

        controller.setItem(row, column, InteractiveItem.previousPage(itemStack(Material.SPECTRAL_ARROW) {
            meta {
                name = translation.displayName("blocko.inventory_utils.next_page_item_display_name")
            }
        }, pagination))
    }

}