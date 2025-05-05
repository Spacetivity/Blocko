package net.spacetivity.blocko.command.api

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommand
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class BukkitCommandExecutor(private val spaceCommand: SpaceCommand, private val plugin: BlockoGame) : CommandExecutor, TabCompleter {

    private val mainCommandExecutor = this.plugin.commandController.getCommandInitializer(this.spaceCommand.name)!!

    init {
        val pluginCommand = this.plugin.getCommand(this.spaceCommand.name)
            ?: throw NullPointerException("Command ${this.spaceCommand.name} is not registered in the plugin.yml of plugin ${this.plugin.name}")

        pluginCommand.setAliases(this.spaceCommand.aliases.toList())
        pluginCommand.setExecutor(this)
        pluginCommand.tabCompleter = this
        pluginCommand.permission = this.spaceCommand.permission

        this.plugin.server.commandMap.register("", pluginCommand)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val spaceCommandSender = SpaceCommandSender(sender)

        if (!checkPermission(spaceCommandSender, sender, this.spaceCommand.permission)) {
            sender.sendMessage(this.plugin.translationHandler.getSelectedTranslation().line("blocko.utils.no_permission"))
            return false
        }

        val arguments = args.toList()

        if (arguments.isEmpty()) {
            this.mainCommandExecutor.defaultExecute(spaceCommandSender)
            return true
        }

        // subCommandParts removes the first element from a command: /perms group Admin info => group Admin info, because 'perms' is the main command!
        val subCommandParts = arguments.drop(0)
        val subCommandData = this.mainCommandExecutor.findValidSubCommandData(subCommandParts)

        if (subCommandData == null) {
            this.mainCommandExecutor.defaultExecute(spaceCommandSender)
            return true
        }

        val subCommandExecutor = subCommandData.first
        val subCommand = subCommandData.second

        if (!checkPermission(spaceCommandSender, sender, subCommand.permission))
            return false

        subCommandExecutor.execute(spaceCommandSender, subCommandParts)
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String?>? {
        val spaceCommandSender = SpaceCommandSender(sender)
        if (!checkPermission(spaceCommandSender, sender, this.spaceCommand.permission)) return emptyList()

        val arguments = args.toList()

        if (arguments.isEmpty() || arguments.size == 1)
            return this.mainCommandExecutor.onDefaultTabComplete(spaceCommandSender, arguments.toList())

        // subCommandParts removes the first element from a command: /perms group Admin info => group Admin info, because 'perms' is the main command!
        val subCommandArgs = arguments.drop(0)

        val suggestions = mutableListOf<String>()

        for (subCommandExecutor in this.mainCommandExecutor.subCommandExecutors) {
            val subCommandAnnotation = subCommandExecutor::class.java.getAnnotation(SpaceSubCommand::class.java)!!
            if (!checkPermission(spaceCommandSender, sender, subCommandAnnotation.permission)) return emptyList()

            suggestions.addAll(subCommandExecutor.onTabComplete(spaceCommandSender, subCommandArgs))
        }

        return suggestions
    }

    private fun checkPermission(spaceCommandSender: SpaceCommandSender, sender: CommandSender, permission: String): Boolean {
        return spaceCommandSender.isPlayer() && (permission == "" || sender.hasPermission(permission))
    }

}