package net.spacetivity.blocko.new_command.api.subcommand

import net.spacetivity.blocko.new_command.api.SpaceCommandCompletable
import net.spacetivity.blocko.new_command.api.SpaceCommandSender

interface SpaceSubCommandExecutor : SpaceCommandCompletable {

    fun execute(sender: SpaceCommandSender, args: List<String>)

    fun onTabComplete(sender: SpaceCommandSender, args: List<String>): List<String>

}