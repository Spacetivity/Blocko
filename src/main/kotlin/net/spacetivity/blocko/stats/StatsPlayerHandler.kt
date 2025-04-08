package net.spacetivity.blocko.stats

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class StatsPlayerHandler {

    val cachedStatsPlayers: MutableList<StatsPlayer> = mutableListOf()

    fun deleteStatsPlayer(uuid: UUID) {
        transaction {
            StatsPlayerDAO.deleteWhere { StatsPlayerDAO.uuid eq uuid.toString() }
        }

        cachedStatsPlayers.removeIf { it.uuid == uuid }
    }

    fun createOrLoadStatsPlayer(uuid: UUID) {
        transaction {
            val resultRow: ResultRow? = StatsPlayerDAO.selectAll().where { StatsPlayerDAO.uuid eq uuid.toString() }.limit(1).firstOrNull()
            val statsPlayer: StatsPlayer

            if (resultRow == null) {
                statsPlayer = StatsPlayer(uuid, 0, 0, 0, 0, 0)
                StatsPlayerDAO.insert { statement: InsertStatement<Number> ->
                    statement[StatsPlayerDAO.uuid] = uuid.toString()
                    statement[eliminatedOpponents] = 0
                    statement[knockedOutByOpponents] = 0
                    statement[playedGames] = 0
                    statement[wonGames] = 0
                    statement[coins] = 0
                }

            } else {
                statsPlayer = StatsPlayer(
                    uuid,
                    resultRow[StatsPlayerDAO.eliminatedOpponents],
                    resultRow[StatsPlayerDAO.knockedOutByOpponents],
                    resultRow[StatsPlayerDAO.playedGames],
                    resultRow[StatsPlayerDAO.wonGames],
                    resultRow[StatsPlayerDAO.coins]
                )
            }

            cachedStatsPlayers.add(statsPlayer)
        }
    }

    fun unloadStatsPlayer(uuid: UUID) {
        val statsPlayer: StatsPlayer = getStatsPlayer(uuid) ?: return
        statsPlayer.updateDbEntry()
        this.cachedStatsPlayers.removeIf { it.uuid == uuid }
    }

    fun updateStatsPlayer(statsPlayer: StatsPlayer) {
        transaction {
            StatsPlayerDAO.update({ StatsPlayerDAO.uuid eq statsPlayer.uuid.toString() }) {
                it[eliminatedOpponents] = statsPlayer.eliminatedOpponents
                it[knockedOutByOpponents] = statsPlayer.knockedOutByOpponents
                it[playedGames] = statsPlayer.playedGames
                it[wonGames] = statsPlayer.wonGames
                it[coins] = statsPlayer.coins
            }
        }
    }

    fun getStatsPlayer(uuid: UUID): StatsPlayer? {
        return this.cachedStatsPlayers.find { it.uuid == uuid }
    }

    suspend fun getStatsPlayerAsync(uuid: UUID): StatsPlayer? = withContext(Dispatchers.IO) {
        var statsPlayer: StatsPlayer? = null

        transaction {
            val resultRow: ResultRow? = StatsPlayerDAO.selectAll().where { StatsPlayerDAO.uuid eq uuid.toString() }.limit(1).firstOrNull()
            if (resultRow != null) {
                statsPlayer = StatsPlayer(
                    uuid,
                    resultRow[StatsPlayerDAO.eliminatedOpponents],
                    resultRow[StatsPlayerDAO.knockedOutByOpponents],
                    resultRow[StatsPlayerDAO.playedGames],
                    resultRow[StatsPlayerDAO.wonGames],
                    resultRow[StatsPlayerDAO.coins]
                )
            }
        }

        statsPlayer
    }

}