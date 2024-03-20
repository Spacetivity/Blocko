package net.spacetivity.blocko.command.api

interface SpaceCommandSender {

    val isPlayer: Boolean
    fun <P> castTo(clazz: Class<P>): P
    fun sendMessages(message: Array<String>)
    fun hasPermissions(permission: String): Boolean

}