package net.spacetivity.blocko.command.api

interface SpaceCommandExecutor {

    fun execute(sender: SpaceCommandSender, args: List<String>)
    fun sendUsage(sender: SpaceCommandSender)
    fun onTabComplete(sender: SpaceCommandSender, args: List<String>): MutableList<String>

}