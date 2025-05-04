package net.spacetivity.blocko.command.api.subcommand

import net.spacetivity.blocko.command.api.SpaceCommandCompletable
import net.spacetivity.blocko.command.api.SpaceCommandSender

interface SpaceSubCommandExecutor : SpaceCommandCompletable {

    fun execute(sender: SpaceCommandSender, args: List<String>)

    fun onTabComplete(sender: SpaceCommandSender, args: List<String>): List<String>

}