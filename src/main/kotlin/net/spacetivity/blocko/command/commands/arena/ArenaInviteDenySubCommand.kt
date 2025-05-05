package net.spacetivity.blocko.command.commands.arena

import net.spacetivity.blocko.arena.getPossibleInvitationDestination
import net.spacetivity.blocko.command.api.SpaceCommandSender
import net.spacetivity.blocko.command.api.extension.findArgument
import net.spacetivity.blocko.command.api.extension.generateSuggestions
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommand
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommandExecutor
import net.spacetivity.blocko.translation.translateMessage
import net.spacetivity.blocko.utils.HelperFunctions
import org.bukkit.entity.Player

@SpaceSubCommand(length = 4, parts = "arena invite deny <id>")
class ArenaInviteDenySubCommand : SpaceSubCommandExecutor {

    override fun execute(sender: SpaceCommandSender, args: List<String>) {
        val player = sender.castTo(Player::class.java) ?: return
        val arenaId = findArgument(player, "id", args, String::class.java) ?: return

        HelperFunctions.validateInvitation(arenaId, player) { gameArena ->
            gameArena.invitedPlayers.remove(player.uniqueId)
            player.translateMessage("blocko.command.arena_invite.invitation_denied")
        }
    }

    override fun onTabComplete(sender: SpaceCommandSender, args: List<String>): List<String> {
        val player = sender.castTo(Player::class.java) ?: return emptyList()

        return buildList {
            addAll(generateSuggestions(args, 3, listOf(Pair(0, "arena"), Pair(1, "invite"))) { add("deny") })
            addAll(generateSuggestions(args, 4, listOf(Pair(0, "arena"), Pair(1, "invite"), Pair(2, "deny"))) {
                val gameArena = player.getPossibleInvitationDestination() ?: return@generateSuggestions
                add(gameArena.id.value)
            })
        }
    }

}