package net.spacetivity.ludo.stats

import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class StatsPlayerHandler {

    private val cachedStatsPlayers: MutableList<StatsPlayer> = mutableListOf()

    @OptIn(DelicateCoroutinesApi::class)
    fun deleteStatsPlayer(uuid: UUID) {
        GlobalScope.launch {
            transaction {
                StatsPlayerDAO.deleteWhere { StatsPlayerDAO.uuid eq uuid.toString() }
            }

            synchronized(cachedStatsPlayers) {
                cachedStatsPlayers.removeIf { it.uuid == uuid }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun createOrLoadStatsPlayer(uuid: UUID) {
        transaction {
            val resultRow: ResultRow? = StatsPlayerDAO.select { StatsPlayerDAO.uuid eq uuid.toString() }.limit(1).firstOrNull()
            val statsPlayer: StatsPlayer

            if (resultRow == null) {
                statsPlayer = StatsPlayer(uuid, 0, 0, 0, 0)
                GlobalScope.launch {
                    StatsPlayerDAO.insert { statement: InsertStatement<Number> ->
                        statement[StatsPlayerDAO.uuid] = uuid.toString()
                        statement[eliminatedOpponents] = 0
                        statement[knockedOutByOpponents] = 0
                        statement[playedGames] = 0
                        statement[coins] = 0
                    }
                }
            } else {
                statsPlayer = StatsPlayer(
                    uuid,
                    resultRow[StatsPlayerDAO.eliminatedOpponents],
                    resultRow[StatsPlayerDAO.knockedOutByOpponents],
                    resultRow[StatsPlayerDAO.playedGames],
                    resultRow[StatsPlayerDAO.coins]
                )
            }

            cachedStatsPlayers.add(statsPlayer)
        }
    }

    fun unloadStatsPlayer(uuid: UUID) {
        this.cachedStatsPlayers.removeIf { it.uuid == uuid }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun updateStatsPlayer(statsPlayer: StatsPlayer) {
        GlobalScope.launch {
            transaction {
                StatsPlayerDAO.update({ StatsPlayerDAO.uuid eq statsPlayer.uuid.toString() }) {
                    it[eliminatedOpponents] = statsPlayer.eliminatedOpponents
                    it[knockedOutByOpponents] = statsPlayer.knockedOutByOpponents
                    it[playedGames] = statsPlayer.playedGames
                    it[coins] = statsPlayer.coins
                }
            }
        }
    }

    fun getCachedStatsPlayer(uuid: UUID): StatsPlayer? {
        return this.cachedStatsPlayers.find { it.uuid == uuid }
    }

    suspend fun getStatsPlayerAsync(uuid: UUID): StatsPlayer? = withContext(Dispatchers.IO) {
        var statsPlayer: StatsPlayer? = null

        transaction {
            val resultRow: ResultRow? = StatsPlayerDAO.select { StatsPlayerDAO.uuid eq uuid.toString() }.limit(1).firstOrNull()
            if (resultRow != null) {
                statsPlayer = StatsPlayer(
                    uuid,
                    resultRow[StatsPlayerDAO.eliminatedOpponents],
                    resultRow[StatsPlayerDAO.knockedOutByOpponents],
                    resultRow[StatsPlayerDAO.playedGames],
                    resultRow[StatsPlayerDAO.coins]
                )
            }
        }

        statsPlayer
    }

}