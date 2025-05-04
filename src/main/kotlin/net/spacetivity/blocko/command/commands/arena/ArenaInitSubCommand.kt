package net.spacetivity.blocko.command.commands.arena

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.command.api.SpaceCommandSender
import net.spacetivity.blocko.command.api.extension.generateSuggestions
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommand
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommandExecutor
import net.spacetivity.blocko.translation.translateMessage
import org.bukkit.entity.Player

@SpaceSubCommand(length = 2, parts = "arena init", permission = "blocko.command.admin")
class ArenaInitSubCommand : SpaceSubCommandExecutor {

    override fun execute(sender: SpaceCommandSender, args: List<String>) {
        val player = sender.castTo(Player::class.java) ?: return
        val creationStatus = BlockoGame.instance.arenaHandler.createArena(player.world.name, player.location)

        if (!creationStatus) {
            val maxArenaCount = BlockoGame.instance.globalConfigFile.gameArenaMaxParallelAmount
            player.translateMessage("blocko.command.blocko.arena_limit_reached", Placeholder.parsed("arena_limit", maxArenaCount.toString()))
            return
        }

        player.translateMessage("blocko.command.blocko.arena_created")
    }

    override fun onTabComplete(sender: SpaceCommandSender, args: List<String>): List<String> {
        return buildList {
            addAll(generateSuggestions(args, 2, listOf(Pair(0, "arena"))) { add("init") })
        }
    }

}