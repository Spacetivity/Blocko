package net.spacetivity.ludo.field

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object GameFieldDAO : Table("game_fields") {
    val id: Column<Int> = integer("id")
    val arenaId: Column<String> = varchar("arenaId", 10)
    val worldName: Column<String> = varchar("worldName", 30)
    val x: Column<Double> = double("x")
    val z: Column<Double> = double("z")
    val turnComponent = varchar("turnComponent", 10).nullable()
    val teamGarageEntrance = varchar("teamGarageEntrance", 10).nullable()
}
