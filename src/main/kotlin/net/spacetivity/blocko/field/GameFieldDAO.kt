package net.spacetivity.blocko.field

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object GameFieldDAO : Table("game_fields") {
    val arenaId: Column<String> = varchar("arenaId", 10)
    val worldName: Column<String> = varchar("worldName", 30)
    val x: Column<Double> = double("x")
    val z: Column<Double> = double("z")
    val properties: Column<String> = text("properties")
    val isGarageField: Column<Boolean> = bool("isGarage")
}