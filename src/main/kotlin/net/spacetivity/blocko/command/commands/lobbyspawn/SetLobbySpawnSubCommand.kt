package net.spacetivity.blocko.command.commands.lobbyspawn

import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.command.api.SpaceCommandSender
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommand
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommandExecutor
import net.spacetivity.blocko.translation.translateMessage

@SpaceSubCommand(length = 3, parts = "setLobbySpawn")
class SetLobbySpawnSubCommand : SpaceSubCommandExecutor {

    override fun execute(sender: SpaceCommandSender, args: List<String>) {
        val player = sender.toPlayer()
        Blocko.instance.lobbySpawnHandler.setLobbySpawn(player.location)
        player.translateMessage("blocko.command.blocko.lobby_spawn_set")
    }

    override fun onTabComplete(sender: SpaceCommandSender, args: List<String>): List<String> = emptyList()

}