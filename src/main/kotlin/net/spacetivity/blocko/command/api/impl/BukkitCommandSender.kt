package net.spacetivity.blocko.command.api.impl

import net.spacetivity.blocko.command.api.SpaceCommandSender
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class BukkitCommandSender(sender: CommandSender) : SpaceCommandSender {

    private val commandSender: CommandSender = sender
    override val isPlayer: Boolean get() = commandSender is Player

    override fun <P> castTo(clazz: Class<P>): P {
        return clazz.cast(commandSender)
    }

    override fun sendMessages(message: Array<String>) {
        commandSender.sendMessage(*message)
    }

    override fun hasPermissions(permission: String): Boolean {
        return commandSender.hasPermission(permission)
    }
}