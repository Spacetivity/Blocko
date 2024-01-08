package net.spacetivity.ludo.command.api.impl

import net.spacetivity.ludo.command.api.LudoCommandSender
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class BukkitCommandSender(sender: CommandSender) : LudoCommandSender {

    private val commandSender: CommandSender
    override val isPlayer: Boolean get() = commandSender is Player

    init {
        commandSender = sender
    }

    override fun <P> castTo(clazz: Class<P>): P {
        return clazz.cast(commandSender)
    }

    override fun sendMessage(message: String) {
        commandSender.sendMessage(message)
    }

    override fun sendMessages(message: Array<String>) {
        commandSender.sendMessage(*message)
    }

    override fun hasPermissions(permission: String): Boolean {
        return commandSender.hasPermission(permission)
    }
}