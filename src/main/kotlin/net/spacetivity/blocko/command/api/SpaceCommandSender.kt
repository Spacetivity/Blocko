package net.spacetivity.blocko.command.api

import net.kyori.adventure.identity.Identity
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

class SpaceCommandSender(private val commandSender: CommandSender) {

    val uuid: UUID = this.commandSender.pointers().get(Identity.UUID).get()

    fun isPlayer(): Boolean = this.commandSender is Player

    fun hasPermission(permission: String): Boolean = this.commandSender.hasPermission(permission)

    fun toPlayer(): Player = Player::class.java.cast(this.commandSender)

    fun sendMessage(component: Component) {
        this.commandSender.sendMessage(component)
    }

}