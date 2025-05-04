package net.spacetivity.blocko.command.commands.arena

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.command.api.SpaceCommandSender
import net.spacetivity.blocko.command.api.extension.generateSuggestions
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommand
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommandExecutor
import net.spacetivity.blocko.translation.translateMessage
import org.bukkit.entity.Player

@SpaceSubCommand(length = 2, parts = "arena list", permission = "blocko.command.admin")
class ArenaListSubCommand : SpaceSubCommandExecutor {

    override fun execute(sender: SpaceCommandSender, args: List<String>) {
        val player = sender.castTo(Player::class.java) ?: return

        val cachedArenas = BlockoGame.Companion.instance.arenaHandler.cachedArenas

        if (cachedArenas.isEmpty()) {
            player.translateMessage("blocko.command.blocko.no_arenas_found")
            return
        }

        player.translateMessage("blocko.command.blocko.arena_list.title")

        for (gameArena in cachedArenas) {
            val currentPlayerAmount = gameArena.currentPlayers.size
            val maxPlayerAmount = gameArena.teamOptions.playerCount

            player.translateMessage("blocko.command.blocko.arena_list.line",
                Placeholder.parsed("id", gameArena.id.value),
                Placeholder.parsed("status", gameArena.status.name),
                Placeholder.parsed("current_player_amount", currentPlayerAmount.toString()),
                Placeholder.parsed("max_player_amount", maxPlayerAmount.toString()))
        }
    }

    override fun onTabComplete(sender: SpaceCommandSender, args: List<String>): List<String> {
        return buildList {
            addAll(generateSuggestions(args, 2, listOf(Pair(0, "arena"))) { add("list") })
        }
    }

}