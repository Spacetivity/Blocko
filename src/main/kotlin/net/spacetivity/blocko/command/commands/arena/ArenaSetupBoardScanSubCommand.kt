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
import net.spacetivity.blocko.utils.HelperFunctions
import org.bukkit.entity.Player

@SpaceSubCommand(length = 5, parts = "arena setup board scan <id>", permission = "blocko.command.admin")
class ArenaSetupBoardScanSubCommand : SpaceSubCommandExecutor {

    override fun execute(sender: SpaceCommandSender, args: List<String>) {
        val player = sender.castTo(Player::class.java) ?: return
        HelperFunctions.checkSetupMode(player) { setupSession ->
            val arenaIdAsString = findArgument(player, "id", args, String::class.java) ?: return@checkSetupMode
            val arenaId = BlockoGame.instance.arenaHandler.getArenaId(arenaIdAsString)
            val gameArena = arenaId?.let { BlockoGame.instance.arenaHandler.getArena(it) }

            if (arenaId == null || gameArena == null) {
                player.translateMessage("blocko.command.blocko.arena_not_exists", Placeholder.parsed("id", arenaIdAsString))
                return@checkSetupMode
            }

            if (gameArena.status != ArenaStatus.CONFIGURATING) {
                player.translateMessage("blocko.command.blocko.arena_fully_configured")
                return@checkSetupMode
            }

            BlockoGame.instance.arenaSetupHandler.scanBoard(player)
        }
    }

    override fun onTabComplete(sender: SpaceCommandSender, args: List<String>): List<String> {
        return buildList {
            addAll(generateSuggestions(args, 4, listOf(Pair(0, "arena"), Pair(1, "setup"), Pair(2, "board"))) { add("scan") })

            addAll(generateSuggestions(args, 5, listOf(Pair(0, "arena"), Pair(1, "setup"), Pair(2, "board"), Pair(3, "scan"))) {
                addAll(BlockoGame.instance.arenaHandler.cachedArenaIds.map { it.value })
            })
        }
    }

}