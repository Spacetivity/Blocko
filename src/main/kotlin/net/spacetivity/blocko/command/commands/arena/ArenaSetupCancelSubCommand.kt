package net.spacetivity.blocko.command.commands.arena

import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.command.api.SpaceCommandSender
import net.spacetivity.blocko.command.api.extension.generateSuggestions
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommand
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommandExecutor
import net.spacetivity.blocko.translation.translateMessage
import net.spacetivity.blocko.utils.HelperFunctions

@SpaceSubCommand(length = 2, parts = "setup cancel", permission = "blocko.command.admin")
class ArenaSetupCancelSubCommand : SpaceSubCommandExecutor {

    override fun execute(sender: SpaceCommandSender, args: List<String>) {
        val player = sender.toPlayer()
        HelperFunctions.checkSetupMode(player) { setupSession ->
            if (Blocko.instance.arenaHandler.cachedArenas.none { it.id == setupSession.arenaId }) {
                player.translateMessage("blocko.command.blocko.arena_not_exists")
                return@checkSetupMode
            }

            Blocko.instance.arenaSetupHandler.handleSetupEnd(player, false)
        }
    }

    override fun onTabComplete(sender: SpaceCommandSender, args: List<String>): List<String> {
        return buildList {
            addAll(generateSuggestions(args, 2, listOf(Pair(0, "setup"))) { add("cancel") })
        }
    }

}