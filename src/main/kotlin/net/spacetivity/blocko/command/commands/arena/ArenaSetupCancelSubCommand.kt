package net.spacetivity.blocko.command.commands.arena

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.command.api.SpaceCommandSender
import net.spacetivity.blocko.command.api.extension.generateSuggestions
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommand
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommandExecutor
import net.spacetivity.blocko.translation.translateMessage
import net.spacetivity.blocko.utils.SetupUtils
import org.bukkit.entity.Player

@SpaceSubCommand(length = 2, parts = "setup cancel", permission = "blocko.command.admin")
class ArenaSetupCancelSubCommand : SpaceSubCommandExecutor {

    override fun execute(sender: SpaceCommandSender, args: List<String>) {
        val player = sender.castTo(Player::class.java) ?: return
        SetupUtils.checkSetupMode(player) {setupSession ->
            if (BlockoGame.instance.arenaHandler.cachedArenas.none { it.id == setupSession.arenaId }) {
                player.translateMessage("blocko.command.blocko.arena_not_exists")
                return@checkSetupMode
            }

            BlockoGame.instance.arenaSetupHandler.handleSetupEnd(player, false)
        }
    }

    override fun onTabComplete(sender: SpaceCommandSender, args: List<String>): List<String> {
        return buildList {
            addAll(generateSuggestions(args, 2, listOf(Pair(0, "setup"))) { add("cancel") })
        }
    }

}