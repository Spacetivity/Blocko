package net.spacetivity.blocko.command.api.impl

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.command.api.CommandProperties
import net.spacetivity.blocko.command.api.SpaceCommandExecutor
import net.spacetivity.blocko.command.api.SpaceCommandSender
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class BukkitCommandExecutor(command: CommandProperties, plugin: BlockoGame) : CommandExecutor, TabCompleter {
    private val commandExecutor: SpaceCommandExecutor
    private val command: CommandProperties

    init {
        commandExecutor = plugin.commandHandler.getCommandExecutor(command.name)!!
        this.command = command

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
            commandSender.castTo(Player::class.java)
                .sendMessage(Component.text("You are not permitted to execute that command!").color(NamedTextColor.RED))
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