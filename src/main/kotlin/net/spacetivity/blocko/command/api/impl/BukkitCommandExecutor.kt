package net.spacetivity.blocko.command.api.impl

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.command.api.CommandProperties
import net.spacetivity.blocko.command.api.SpaceCommandExecutor
import net.spacetivity.blocko.command.api.SpaceCommandSender
import net.spacetivity.blocko.extensions.translateMessage
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class BukkitCommandExecutor(private val command: CommandProperties, plugin: BlockoGame) : CommandExecutor, TabCompleter {

    private val commandExecutor: SpaceCommandExecutor = plugin.commandHandler.getCommandExecutor(command.name)!!

    init {

        val pluginCommand: Command = object : Command(command.name) {
            override fun execute(sender: CommandSender, commandLabel: String, args: Array<String>): Boolean {
                return onCommand(sender, this, commandLabel, args)
            }

            override fun tabComplete(
                sender: CommandSender,
                alias: String,
                args: Array<String>
            ): MutableList<String> {
                return onTabComplete(sender, this, alias, args)
            }
        }

        pluginCommand.aliases = command.aliases.toMutableList()
        pluginCommand.permission = command.permission
        plugin.server.commandMap.register("", pluginCommand)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        val commandSender: SpaceCommandSender = BukkitCommandSender(sender)
        if (commandSender.isPlayer && this.command.permission.isNotBlank() && !sender.hasPermission(this.command.permission)) {
            commandSender.castTo(Player::class.java).translateMessage("blocko.utils.no_permission")
            return true
        }

        commandExecutor.execute(commandSender, listOf(*args))
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): MutableList<String> {
        val commandSender: SpaceCommandSender = BukkitCommandSender(sender)
        return commandExecutor.onTabComplete(commandSender, args.toMutableList())
    }
}