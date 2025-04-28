package net.spacetivity.blocko.stats

import org.jetbrains.exposed.sql.Table

object StatsPlayerDAO : Table("stats_players") {
    val uuid = varchar("uuid", 36)
    val eliminatedOpponents = integer("eliminatedOpponents")
    val knockedOutByOpponents = integer("knockedOutByOpponents")
    val playedGames = integer("playedGames")
    val wonGames = integer("wonGames")
    val coins = integer("coins")

    override val primaryKey = PrimaryKey(uuid)
}