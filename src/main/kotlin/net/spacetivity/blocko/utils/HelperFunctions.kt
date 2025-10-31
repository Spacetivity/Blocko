package net.spacetivity.blocko.utils

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.arena.Arena
import net.spacetivity.blocko.setup.ArenaSetupSession
import net.spacetivity.blocko.setup.getSetupSession
import net.spacetivity.blocko.translation.translateMessage
import org.bukkit.entity.Player

object HelperFunctions {

    fun checkSetupMode(player: Player, result: (ArenaSetupSession) -> Unit) {
        val setupSession = player.getSetupSession()
        if (setupSession == null) {
            player.translateMessage("blocko.setup.not_in_setup_mode")
            return
        }

        result.invoke(setupSession)
    }

    fun validateInvitation(arenaIdAsString: String, player: Player, result: (Arena) -> Unit) {
        val arenaHandler = Blocko.instance.arenaHandler

        val arenaId = arenaHandler.getArenaId(arenaIdAsString)
        val gameArena = arenaId?.let { arenaHandler.getArena(it) }

        if (arenaId == null || gameArena == null) {
            player.translateMessage("blocko.command.blocko.arena_not_exists", Placeholder.parsed("id", arenaIdAsString))
            return
        }

        if (!gameArena.invitedPlayers.contains(player.uniqueId)) {
            player.translateMessage("blocko.command.arena_invite.no_open_invitation")
            return
        }

        if (!gameArena.phase.isIdle()) {
            player.translateMessage("blocko.command.arena_invite.invitation_expired")
            return
        }

        result.invoke(gameArena)
    }

}

/**
 * Formats a team name by converting to lowercase and capitalizing the first letter.
 * Example: "RED" -> "Red", "green" -> "Green"
 */
fun String.formatTeamName(): String {
    return this.lowercase().replaceFirstChar { it.uppercase() }
}