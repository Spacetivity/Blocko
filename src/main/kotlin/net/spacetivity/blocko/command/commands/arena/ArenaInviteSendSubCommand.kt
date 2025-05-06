package net.spacetivity.blocko.command.commands.arena

import net.spacetivity.blocko.arena.getArena
import net.spacetivity.blocko.arena.toGamePlayerInstance
import net.spacetivity.blocko.command.api.SpaceCommandSender
import net.spacetivity.blocko.command.api.extension.findArgument
import net.spacetivity.blocko.command.api.extension.generateSuggestions
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommand
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommandExecutor
import net.spacetivity.blocko.translation.translateMessage
import org.bukkit.Bukkit

@SpaceSubCommand(length = 4, parts = "arena invite send <player>")
class ArenaInviteSendSubCommand : SpaceSubCommandExecutor {

    override fun execute(sender: SpaceCommandSender, args: List<String>) {
        val player = sender.toPlayer()
        val gameArena = player.getArena()

        if (gameArena == null) {
            player.translateMessage("blocko.command.arena_invite.not_in_a_game")
            return
        }

        if (gameArena.arenaHost != null && gameArena.arenaHost!!.uuid != player.uniqueId) {
            player.translateMessage("blocko.command.arena_invite.not_the_host_player")
            return
        }

        val name = findArgument(player, "player", args, String::class.java)?:return
        val gamePlayer = player.toGamePlayerInstance() ?: return
        gameArena.sendArenaInvite(gamePlayer, name)
        return
    }

    override fun onTabComplete(sender: SpaceCommandSender, args: List<String>): List<String> {
        val player = if (sender.isPlayer()) sender.toPlayer() else return emptyList()

        return buildList {
            addAll(generateSuggestions(args, 2, listOf(Pair(0, "arena"))) { add("invite") })
            addAll(generateSuggestions(args, 3, listOf(Pair(0, "arena"), Pair(1, "invite"))) { add("send") })
            addAll(generateSuggestions(args, 4, listOf(Pair(0, "arena"), Pair(1, "invite"), Pair(2, "send"))) {
                addAll(Bukkit.getOnlinePlayers().filter { it.getArena() == null && it.uniqueId != player.uniqueId }.map { it.name })
            })
        }
    }

}