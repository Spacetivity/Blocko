package net.spacetivity.blocko.arena.sign

import org.jetbrains.exposed.sql.Table

object ArenaSignDAO : Table("game_arena_signs") {
    val worldName = varchar("worldName", 50)
    val x = double("x")
    val y = double("y")
    val z = double("z")
}