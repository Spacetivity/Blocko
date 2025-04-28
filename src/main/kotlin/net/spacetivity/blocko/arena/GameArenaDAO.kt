package net.spacetivity.blocko.arena

import org.jetbrains.exposed.sql.Table

object GameArenaDAO : Table("game_arenas") {
    val id = varchar("id", 10)
    val worldName = varchar("worldName", 100)
    val playerLocation = text("playerLocation")
    val maxPlayers = integer("maxPlayers")
    val status = varchar("arenaStatus", 30)

    override val primaryKey = PrimaryKey(id)
}