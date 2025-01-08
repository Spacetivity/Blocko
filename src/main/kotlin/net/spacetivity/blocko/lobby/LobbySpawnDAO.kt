package net.spacetivity.blocko.lobby

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object LobbySpawnDAO : Table("lobby_settings") {
    val worldName: Column<String> = varchar("worldName", 30)
    val x: Column<Double> = double("x")
    val y: Column<Double> = double("y")
    val z: Column<Double> = double("z")
    val yaw: Column<Float> = float("yaw")
    val pitch: Column<Float> = float("pitch")
}