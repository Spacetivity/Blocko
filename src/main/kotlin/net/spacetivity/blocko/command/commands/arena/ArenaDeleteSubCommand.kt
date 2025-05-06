package net.spacetivity.blocko.command.commands.arena

import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.command.api.SpaceCommandSender
import net.spacetivity.blocko.command.api.extension.findArgument
import net.spacetivity.blocko.command.api.extension.generateSuggestions
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommand
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommandExecutor
import net.spacetivity.blocko.translation.translateMessage

@SpaceSubCommand(length = 3, parts = "arena delete <id>", permission = "blocko.command.admin")
class ArenaDeleteSubCommand : SpaceSubCommandExecutor {

    private val arenaHandler = Blocko.instance.arenaHandler

    override fun execute(sender: SpaceCommandSender, args: List<String>) {
        val player = sender.toPlayer()
        val arenaIdAsString: String = findArgument(player, "id", args, String::class.java) ?: return
        val arenaId = this.arenaHandler.getArenaId(arenaIdAsString) ?: return

        if (this.arenaHandler.cachedArenas.none { it.id == arenaId }) {
            player.translateMessage("blocko.command.blocko.arena_not_exists")
            return
        }

        this.arenaHandler.deleteArena(arenaId)
        player.translateMessage("blocko.command.blocko.arena_deleted")
    }

    override fun onTabComplete(sender: SpaceCommandSender, args: List<String>): List<String> {
        return buildList {
            addAll(generateSuggestions(args, 2, listOf(Pair(0, "arena"))) { add("delete") })

            addAll(generateSuggestions(args, 3, listOf(Pair(0, "arena"), Pair(1, "delete"))) {
                addAll(arenaHandler.cachedArenaIds.map { it.value })
            })
        }
    }

}