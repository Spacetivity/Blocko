package net.spacetivity.blocko.command.commands.arena

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.ArenaStatus
import net.spacetivity.blocko.command.api.SpaceCommandSender
import net.spacetivity.blocko.command.api.extension.findArgument
import net.spacetivity.blocko.command.api.extension.generateSuggestions
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommand
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommandExecutor
import net.spacetivity.blocko.translation.translateMessage
import org.bukkit.entity.Player

@SpaceSubCommand(length = 3, parts = "arena delete <id>", permission = "blocko.command.admin")
class ArenaDeleteSubCommand : SpaceSubCommandExecutor {

    private val arenaHandler = BlockoGame.instance.arenaHandler

    override fun execute(sender: SpaceCommandSender, args: List<String>) {
        val player = sender.castTo(Player::class.java) ?: return

        val arenaIdAsString = findArgument(player, "id", args, String::class.java) ?: return
        val arenaId = this.arenaHandler.getArenaId(arenaIdAsString)
        val gameArena = arenaId?.let { this.arenaHandler.getArena(it) }

        if (arenaId == null || gameArena == null) {
            player.translateMessage("blocko.command.blocko.arena_not_exists", Placeholder.parsed("id", arenaIdAsString))
            return
        }

        if (gameArena.status != ArenaStatus.CONFIGURATING) {
            player.translateMessage("blocko.command.blocko.arena_fully_configured")
            return
        }

        BlockoGame.instance.arenaSetupHandler.startSetup(player, arenaId)
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