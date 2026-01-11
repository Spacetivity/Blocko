package net.spacetivity.blocko.utils

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.arena.Arena
import net.spacetivity.blocko.arena.getArena
import net.spacetivity.blocko.field.GameField
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
import net.spacetivity.inventorylib.api.GuiProvider
import net.spacetivity.inventorylib.api.extension.openStaticInventory
import net.spacetivity.inventorylib.api.inventory.GuiController
import net.spacetivity.inventorylib.api.item.GuiItem
import org.bukkit.Material
import org.bukkit.entity.Player

object InventoryUtils {

    fun openGameFieldTurnInventory(opener: Player, gameField: GameField?, isTeamEntrance: Boolean = false) {
        val title = Blocko.instance.translationHandler.getSelectedTranslation().line("blocko.inventory.game_field_set_turn.title")
        openStaticInventory(opener, title, GameFieldTurnSetupInventory(gameField, isTeamEntrance))
    }

    fun openGameTeamSetupInventory(opener: Player, invType: InvType, gameField: GameField?) {
        val title = Blocko.instance.translationHandler.getSelectedTranslation().line("blocko.inventory.game_team_setup.title.entrance")
        openStaticInventory(opener, title, GameTeamSetupInventory(invType, gameField))
    }

    fun openHostSettingsInventory(opener: Player, arena: Arena) {
        val title = Blocko.instance.translationHandler.getSelectedTranslation().line("blocko.inventory.host.title")
        openStaticInventory(opener, title, HostSettingsInventory(arena))
    }

    fun openInvitationInventory(opener: Player, arena: Arena) {
        val title = Blocko.instance.translationHandler.getSelectedTranslation().line("blocko.inventory.invitation.title")
        openStaticInventory(opener, title, InvitationInventory(arena))
    }

    fun openTeamSelectorInventory(opener: Player, arena: Arena) {
        val title = Blocko.instance.translationHandler.getSelectedTranslation().line("blocko.inventory.team_selector.title")
        openStaticInventory(opener, title, TeamSelectorInventory(arena))
    }

    fun openProfileInventory(opener: Player, isShopItemActive: Boolean) {
        val title = Blocko.instance.translationHandler.getSelectedTranslation().line("blocko.inventory.profile.title")
        openStaticInventory(opener, title, ProfileInventory(isShopItemActive))
    }

    fun openStatsTeamSelectorInventory(opener: Player) {
        val title = Blocko.instance.translationHandler.getSelectedTranslation().line("blocko.inventory.stats_team_selector.title")
        val gameArena = opener.getArena() ?: return
        openStaticInventory(opener, title, StatsTeamSelectorInventory(gameArena))
    }

    fun openStatsInventory(opener: Player, statsPlayer: StatsPlayer) {
        val translation = Blocko.instance.translationHandler.getSelectedTranslation()

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
        val title = Blocko.instance.translationHandler.getSelectedTranslation().line("blocko.inventory.achievements.title")
        openStaticInventory(opener, title, AchievementsInventory())
    }

    fun openEntityShopInventory(opener: Player) {
        val title = Blocko.instance.translationHandler.getSelectedTranslation().line("blocko.inventory.entity_shop.title")
        openStaticInventory(opener, title, EntityShopInventory())
    }

    fun setPreviousPageItem(row: Int, column: Int, controller: GuiController) {
        val translation = Blocko.instance.translationHandler.getSelectedTranslation()
        val pagination = controller.pagination ?: return

        controller.setItem(row, column, GuiProvider.api.previousPage(itemStack(Material.ARROW) {
            meta {
                name = translation.displayName("blocko.inventory_utils.previous_page_item_display_name")
            }
        }, pagination))
    }

    fun setNextPageItem(row: Int, column: Int, controller: GuiController) {
        val translation = Blocko.instance.translationHandler.getSelectedTranslation()
        val pagination = controller.pagination ?: return

        controller.setItem(row, column, GuiProvider.api.nextPage(itemStack(Material.SPECTRAL_ARROW) {
            meta {
                name = translation.displayName("blocko.inventory_utils.next_page_item_display_name")
            }
        }, pagination))
    }

}