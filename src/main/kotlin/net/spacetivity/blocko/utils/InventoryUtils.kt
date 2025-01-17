package net.spacetivity.blocko.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.extensions.getArena
import net.spacetivity.blocko.inventory.host.HostSettingsInventory
import net.spacetivity.blocko.inventory.host.InvitationInventory
import net.spacetivity.blocko.inventory.profile.*
import net.spacetivity.blocko.inventory.setup.GameFieldTurnSetupInventory
import net.spacetivity.blocko.inventory.setup.GameTeamSetupInventory
import net.spacetivity.blocko.inventory.setup.InvType
import net.spacetivity.blocko.inventory.team.TeamSelectorInventory
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.stats.StatsPlayer
import net.spacetivity.inventory.api.extension.openStaticInventory
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Player

object InventoryUtils {

    fun openGameFieldTurnInventory(opener: Player, location: Location) {
        val title: Component = BlockoGame.instance.translationHandler.getSelectedTranslation().validateLine("blocko.inventory.game_field_set_turn.title")
        openStaticInventory(opener, title, GameFieldTurnSetupInventory(location))
    }

    fun openGameTeamSetupInventory(opener: Player, invType: InvType, block: Block) {
        if (invType == InvType.IDS) throw UnsupportedOperationException("Only select the inv types GARAGE or ENTRANCE!")

        val suffix = if (invType == InvType.GARAGE) "garage" else if (invType == InvType.ENTRANCE) "entrance" else null
        val title: Component = BlockoGame.instance.translationHandler.getSelectedTranslation().validateLine("blocko.inventory.game_team_setup.title.${suffix}")

        openStaticInventory(opener, title, GameTeamSetupInventory(invType, block.location))
    }

    fun openHostSettingsInventory(opener: Player, gameArena: GameArena) {
        val title: Component = BlockoGame.instance.translationHandler.getSelectedTranslation().validateLine("blocko.inventory.host.title")
        openStaticInventory(opener, title, HostSettingsInventory(gameArena))
    }

    fun openInvitationInventory(opener: Player, gameArena: GameArena) {
        val title: Component = BlockoGame.instance.translationHandler.getSelectedTranslation().validateLine("blocko.inventory.invitation.title")
        openStaticInventory(opener, title, InvitationInventory(gameArena))
    }

    fun openTeamSelectorInventory(opener: Player, gameArena: GameArena) {
        val title: Component = BlockoGame.instance.translationHandler.getSelectedTranslation().validateLine("blocko.inventory.team_selector.title")
        openStaticInventory(opener, title, TeamSelectorInventory(gameArena))
    }

    fun openProfileInventory(opener: Player, isShopItemActive: Boolean) {
        val title: Component = BlockoGame.instance.translationHandler.getSelectedTranslation().validateLine("blocko.inventory.profile.title")
        openStaticInventory(opener, title, ProfileInventory(isShopItemActive))
    }

    fun openStatsTeamSelectorInventory(opener: Player) {
        val title: Component = BlockoGame.instance.translationHandler.getSelectedTranslation().validateLine("blocko.inventory.stats_team_selector.title")
        val gameArena: GameArena = opener.getArena() ?: return
        openStaticInventory(opener, title, StatsTeamSelectorInventory(gameArena))
    }

    fun openStatsInventory(opener: Player, statsPlayer: StatsPlayer) {
        val gameArena: GameArena = opener.getArena() ?: return
        val gamePlayer: GamePlayer = gameArena.currentPlayers.find { it.uuid == statsPlayer.uuid } ?: return

        val suffixPlaceholder: TagResolver = if (gamePlayer.isAI) Placeholder.parsed("suffix", "(AI)") else Placeholder.parsed("suffix", "")

        val titleKey = "blocko.inventory.stats.title.${if (opener.uniqueId == statsPlayer.uuid) "matching_name" else "other_name"}"
        val title: Component = BlockoGame.instance.translationHandler.getSelectedTranslation().validateLine(titleKey, Placeholder.parsed("player_name", gamePlayer.name), suffixPlaceholder)

        val showSearchPlayerItem: Boolean = gameArena.phase.isIngame()
        openStaticInventory(opener, title, StatsInventory(gameArena, statsPlayer, showSearchPlayerItem))
    }

    fun openAchievementsInventory(opener: Player) {
        val title: Component = BlockoGame.instance.translationHandler.getSelectedTranslation().validateLine("blocko.inventory.achievements.title")
        openStaticInventory(opener, title, AchievementsInventory())
    }

    fun openEntityShopInventory(opener: Player) {
        val title: Component = BlockoGame.instance.translationHandler.getSelectedTranslation().validateLine("blocko.inventory.entity_shop.title")
        openStaticInventory(opener, title, EntityShopInventory())
    }

}