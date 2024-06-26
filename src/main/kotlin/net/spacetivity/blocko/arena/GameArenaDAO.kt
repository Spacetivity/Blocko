package net.spacetivity.blocko.arena

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object GameArenaDAO : Table("game_arenas") {
    val id: Column<String> = varchar("id", 10)
    val worldName: Column<String> = varchar("worldName", 100)
    val playerLocation: Column<String> = text("playerLocation")
    val maxPlayers: Column<Int> = integer("maxPlayers")
    val status: Column<String> = varchar("arenaStatus", 30)

    override val primaryKey = PrimaryKey(id)
}