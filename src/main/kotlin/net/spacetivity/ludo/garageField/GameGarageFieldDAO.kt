package net.spacetivity.ludo.garageField

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object GameGarageFieldDAO : Table("game_garage_fields") {
    val id: Column<Int> = integer("id")
    val arenaId: Column<String> = varchar("arenaId", 10)
    val teamName: Column<String> = varchar("teamName", 30)
    val worldName: Column<String> = varchar("worldName", 30)
    val x: Column<Double> = double("x")
    val z: Column<Double> = double("z")
}
