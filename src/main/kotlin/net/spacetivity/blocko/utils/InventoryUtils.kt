package net.spacetivity.blocko.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.inventory.api.extension.openStaticInventory
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.extensions.getArena
import net.spacetivity.blocko.inventory.host.HostSettingsInventory
import net.spacetivity.blocko.inventory.host.InvitationInventory
import net.spacetivity.blocko.inventory.profile.*
import net.spacetivity.blocko.inventory.team.TeamSelectorInventory
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.stats.StatsPlayer
import org.bukkit.entity.Player

object InventoryUtils {

    fun openHostSettingsInventory(opener: Player, gameArena: GameArena) {
        val title: Component = BlockoGame.instance.translationHandler.getSelectedTranslation().validateLine("blocko.inventory.host.title")
        BlockoGame.instance.server.openStaticInventory(opener, title, HostSettingsInventory(gameArena))
    }

    fun openInvitationInventory(opener: Player, gameArena: GameArena) {
        val title: Component = BlockoGame.instance.translationHandler.getSelectedTranslation().validateLine("blocko.inventory.invitation.title")
        BlockoGame.instance.server.openStaticInventory(opener, title, InvitationInventory(gameArena))
    }

    fun openTeamSelectorInventory(opener: Player, gameArena: GameArena) {
        val title: Component = BlockoGame.instance.translationHandler.getSelectedTranslation().validateLine("blocko.inventory.team_selector.title")
        BlockoGame.instance.server.openStaticInventory(opener, title, TeamSelectorInventory(gameArena))
    }

    fun openProfileInventory(opener: Player, isShopItemActive: Boolean) {
        val title: Component = BlockoGame.instance.translationHandler.getSelectedTranslation().validateLine("blocko.inventory.profile.title")
        BlockoGame.instance.server.openStaticInventory(opener, title, ProfileInventory(isShopItemActive))
    }

    fun openStatsTeamSelectorInventory(opener: Player) {
        val title: Component = BlockoGame.instance.translationHandler.getSelectedTranslation().validateLine("blocko.inventory.stats_team_selector.title")
        val gameArena: GameArena = opener.getArena() ?: return
        BlockoGame.instance.server.openStaticInventory(opener, title, StatsTeamSelectorInventory(gameArena))
    }

    fun openStatsInventory(opener: Player, statsPlayer: StatsPlayer) {
        val gameArena: GameArena = opener.getArena() ?: return
        val gamePlayer: GamePlayer = gameArena.currentPlayers.find { it.uuid == statsPlayer.uuid } ?: return

        val suffixPlaceholder: TagResolver = if (gamePlayer.isAI) Placeholder.parsed("suffix", "(AI)") else TagResolver.empty()

        val titleKey = "blocko.inventory.stats.title.${if (opener.uniqueId == statsPlayer.uuid) "matching_name" else "other_name"}"
        val title: Component = BlockoGame.instance.translationHandler.getSelectedTranslation().validateLine(titleKey, Placeholder.parsed("player_name", gamePlayer.name), suffixPlaceholder)

        val showSearchPlayerItem: Boolean = gameArena.phase.isIngame()
        BlockoGame.instance.server.openStaticInventory(opener, title, StatsInventory(gameArena, statsPlayer, showSearchPlayerItem))
    }

    fun openAchievementsInventory(opener: Player) {
        val title: Component = BlockoGame.instance.translationHandler.getSelectedTranslation().validateLine("blocko.inventory.achievements.title")
        BlockoGame.instance.server.openStaticInventory(opener, title, AchievementsInventory())
    }

    fun openEntityShopInventory(opener: Player) {
        val title: Component = BlockoGame.instance.translationHandler.getSelectedTranslation().validateLine("blocko.inventory.entity_shop.title")
        BlockoGame.instance.server.openStaticInventory(opener, title, EntityShopInventory())
    }

}