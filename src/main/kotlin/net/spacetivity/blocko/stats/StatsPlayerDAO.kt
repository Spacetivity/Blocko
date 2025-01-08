package net.spacetivity.blocko.stats

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object StatsPlayerDAO : Table("stats_players") {
    val uuid: Column<String> = varchar("uuid", 36)
    val eliminatedOpponents: Column<Int> = integer("eliminatedOpponents")
    val knockedOutByOpponents: Column<Int> = integer("knockedOutByOpponents")
    val playedGames: Column<Int> = integer("playedGames")
    val wonGames: Column<Int> = integer("wonGames")
    val coins: Column<Int> = integer("coins")

    override val primaryKey: PrimaryKey = PrimaryKey(uuid)
}