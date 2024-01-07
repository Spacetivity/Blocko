package net.spacetivity.ludo.arena

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object GameArenaDAO : Table("game_arenas") {
    val id: Column<String> = varchar("id", 10).uniqueIndex()
    val worldName: Column<String> = varchar("worldName", 30)
    val playerLocation: Column<String> = varchar("playerLocation", 30)
    val maxPlayers: Column<Int> = integer("maxPlayers")
}