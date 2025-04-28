package net.spacetivity.blocko.team

import org.jetbrains.exposed.sql.Table

object GameTeamLocationDAO : Table("game_team_locations") {
    val arenaId = varchar("arenaId", 30)
    val teamName = varchar("teamName", 30)
    val worldName = varchar("worldName", 50)
    val x = double("x")
    val y = double("y")
    val z = double("z")
    val yaw = float("yaw")
    val pitch = float("pitch")
}