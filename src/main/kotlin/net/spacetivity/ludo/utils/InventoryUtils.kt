package net.spacetivity.ludo.utils

import net.kyori.adventure.text.Component
import net.spacetivity.inventory.api.extension.openStaticInventory
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.inventory.host.HostSettingsInventory
import net.spacetivity.ludo.inventory.host.InvitationInventory
import net.spacetivity.ludo.inventory.profile.ProfileInventory
import net.spacetivity.ludo.inventory.team.TeamSelectorInventory
import org.bukkit.entity.Player

object InventoryUtils {

    fun openHostSettingsInventory(opener: Player, gameArena: GameArena) {
        val title: Component = LudoGame.instance.translationHandler.getSelectedTranslation().validateLine("blocko.inventory.host.title")
        LudoGame.instance.server.openStaticInventory(opener, title, HostSettingsInventory(gameArena))
    }

    fun openInvitationInventory(opener: Player, gameArena: GameArena) {
        val title: Component = LudoGame.instance.translationHandler.getSelectedTranslation().validateLine("blocko.inventory.invitation.title")
        LudoGame.instance.server.openStaticInventory(opener, title, InvitationInventory(gameArena))
    }

    fun openProfileInventory(opener: Player) {
        LudoGame.instance.server.openStaticInventory(opener, Component.text("Your profile"), ProfileInventory())
    }

    fun openTeamSelectorInventory(opener: Player, gameArena: GameArena) {
        val title: Component = LudoGame.instance.translationHandler.getSelectedTranslation().validateLine("blocko.inventory.team_selector.title")
        LudoGame.instance.server.openStaticInventory(opener, title, TeamSelectorInventory(gameArena))
    }

}