package net.spacetivity.blocko.command.commands

import net.spacetivity.blocko.command.api.SpaceCommand
import net.spacetivity.blocko.command.api.SpaceCommandSender
import net.spacetivity.blocko.command.api.SpaceMainCommandExecutor
import net.spacetivity.blocko.command.api.extension.generateSimpleSuggestions
import net.spacetivity.blocko.command.api.extension.generateSuggestions
import net.spacetivity.blocko.command.api.extension.sendUsageFormatted
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommandExecutor
import net.spacetivity.blocko.command.commands.arena.*
import net.spacetivity.blocko.command.commands.lobbyspawn.SetLobbySpawnSubCommand
import net.spacetivity.blocko.command.commands.teleport.WorldTpSubCommand

@SpaceCommand(name = "blocko", title = "Blocko Game", permission = "blocko.command.user")
class BlockoCommand : SpaceMainCommandExecutor() {

    override fun defaultExecute(sender: SpaceCommandSender) {
        sendUsageFormatted(sender)
    }

    override fun initSubCommands(subCommandExecutors: MutableList<SpaceSubCommandExecutor>) {
        // setLobbySpawn
        subCommandExecutors.add(SetLobbySpawnSubCommand())

        // arena
        subCommandExecutors.add(ArenaListSubCommand())
        subCommandExecutors.add(ArenaInitSubCommand())
        subCommandExecutors.add(ArenaDeleteSubCommand())
        subCommandExecutors.add(ArenaSetupStartSubCommand())
        subCommandExecutors.add(ArenaSetupBoardScanSubCommand())
        subCommandExecutors.add(ArenaSetupBoardCheckSubCommand())
        subCommandExecutors.add(ArenaSetupCancelSubCommand())
        subCommandExecutors.add(ArenaSetupFinishSubCommand())

        subCommandExecutors.add(ArenaInviteSendSubCommand())

        // worldTp
        subCommandExecutors.add(WorldTpSubCommand())
    }

    override fun onDefaultTabComplete(sender: SpaceCommandSender, args: List<String>): List<String> {
        return buildList {
            addAll(generateSimpleSuggestions(args, 0) { add("setLobbySpawn") })
            addAll(generateSuggestions(args, 1) { add("setLobbySpawn") })

            addAll(generateSimpleSuggestions(args, 0) { add("arena") })
            addAll(generateSuggestions(args, 1) { add("arena") })

            addAll(generateSimpleSuggestions(args, 0) { add("worldTp") })
            addAll(generateSuggestions(args, 1) { add("worldTp") })
        }
    }

}