package net.spacetivity.blocko.command.commands.arena

import net.spacetivity.blocko.arena.getPossibleInvitationDestination
import net.spacetivity.blocko.command.api.SpaceCommandSender
import net.spacetivity.blocko.command.api.extension.findArgument
import net.spacetivity.blocko.command.api.extension.generateSuggestions
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommand
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommandExecutor
import net.spacetivity.blocko.utils.HelperFunctions
import org.bukkit.entity.Player

@SpaceSubCommand(length = 4, parts = "arena invite accept <id>")
class ArenaInviteAcceptSubCommand : SpaceSubCommandExecutor {

    override fun execute(sender: SpaceCommandSender, args: List<String>) {
        val player = sender.castTo(Player::class.java) ?: return
        val arenaId = findArgument(player, "id", args, String::class.java) ?: return

        HelperFunctions.validateInvitation(arenaId, player) { gameArena ->
            val wasJoinSuccessful = gameArena.join(player.uniqueId, false)
            if (wasJoinSuccessful) gameArena.invitedPlayers.remove(player.uniqueId)
        }
    }

    override fun onTabComplete(sender: SpaceCommandSender, args: List<String>): List<String> {
        val player = sender.castTo(Player::class.java) ?: return emptyList()

        return buildList {
            addAll(generateSuggestions(args, 3, listOf(Pair(0, "arena"), Pair(1, "invite"))) { add("accept") })
            addAll(generateSuggestions(args, 4, listOf(Pair(0, "arena"), Pair(1, "invite"), Pair(2, "accept"))) {
                val gameArena = player.getPossibleInvitationDestination() ?: return@generateSuggestions
                add(gameArena.id.value)
            })
        }
    }

}