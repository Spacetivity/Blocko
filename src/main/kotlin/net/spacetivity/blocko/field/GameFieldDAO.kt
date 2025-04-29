package net.spacetivity.blocko.field

import net.spacetivity.blocko.arena.id.arenaId
import org.jetbrains.exposed.sql.Table

object GameFieldDAO : Table("game_fields") {
    val arenaId = arenaId("arenaId", 10)
    val worldName = varchar("worldName", 30)
    val x = double("x")
    val z = double("z")
    val properties = text("properties")
    val isGarageField = bool("isGarage")
}