package net.spacetivity.ludo.command.api

interface LudoCommandExecutor {

    fun execute(sender: LudoCommandSender, args: List<String>)
    fun sendUsage(sender: LudoCommandSender)
    fun onTabComplete(sender: LudoCommandSender, args: List<String>): MutableList<String>

}