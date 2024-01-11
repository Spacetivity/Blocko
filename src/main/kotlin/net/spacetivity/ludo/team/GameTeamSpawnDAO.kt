package net.spacetivity.ludo.team

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object GameTeamSpawnDAO : Table("game_team_spawns") {
    val arenaId: Column<String> = varchar("arenaId", 30)
    val teamName: Column<String> = varchar("teamName", 30)
    val worldName: Column<String> = varchar("worldName", 50)
    val x: Column<Double> = double("x")
    val y: Column<Double> = double("y")
    val z: Column<Double> = double("z")
    val yaw: Column<Float> = float("yaw")
    val pitch: Column<Float> = float("pitch")
}