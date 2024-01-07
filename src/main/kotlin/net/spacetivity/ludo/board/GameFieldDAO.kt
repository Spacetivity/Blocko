package net.spacetivity.ludo.board

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object GameFieldDAO : Table("game_fields") {
    val arenaId: Column<String> = varchar("arenaId", 10)
    val worldName: Column<String> = varchar("worldName", 30)
    val x: Column<Double> = double("x")
    val z: Column<Double> = double("z")
    val turnComponent = varchar("turnComponent", 10).nullable()
}
