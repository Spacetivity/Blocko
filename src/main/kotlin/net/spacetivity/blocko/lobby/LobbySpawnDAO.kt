package net.spacetivity.blocko.lobby

import org.jetbrains.exposed.sql.Table

object LobbySpawnDAO : Table("lobby_settings") {
    val worldName = varchar("worldName", 30)
    val x = double("x")
    val y = double("y")
    val z = double("z")
    val yaw = float("yaw")
    val pitch = float("pitch")
}