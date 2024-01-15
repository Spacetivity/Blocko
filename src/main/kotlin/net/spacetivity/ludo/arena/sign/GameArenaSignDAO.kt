package net.spacetivity.ludo.arena.sign

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object GameArenaSignDAO : Table("game_arena_signs") {
    val worldName: Column<String> = varchar("worldName", 50)
    val x: Column<Double> = double("x")
    val y: Column<Double> = double("y")
    val z: Column<Double> = double("z")
}