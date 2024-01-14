package net.spacetivity.ludo.garageField

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import org.bukkit.Bukkit
import org.bukkit.World
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.transaction

class GameGarageFieldHandler {

    val cachedGarageFields: Multimap<String, GameGarageField> = ArrayListMultimap.create()

    init {
        transaction {
            for (resultRow: ResultRow in GameGarageFieldDAO.selectAll().toMutableList()) {
                val id: Int = resultRow[GameGarageFieldDAO.id]
                val arenaId: String = resultRow[GameGarageFieldDAO.arenaId]
                val teamName: String = resultRow[GameGarageFieldDAO.teamName]
                val world: World = Bukkit.getWorld(resultRow[GameGarageFieldDAO.worldName]) ?: continue
                val x: Double = resultRow[GameGarageFieldDAO.x]
                val z: Double = resultRow[GameGarageFieldDAO.z]
                cachedGarageFields.put(arenaId, GameGarageField(id, arenaId, teamName, world, x, z))
            }
        }
    }

    fun getGarageField(arenaId: String, teamName: String, id: Int): GameGarageField? {
        return this.cachedGarageFields.get(arenaId).find { it.id == id && it.teamName.equals(teamName, true) }
    }

    fun deleteGarageFields(arenaId: String) {
        transaction { GameGarageFieldDAO.deleteWhere { GameGarageFieldDAO.arenaId eq arenaId } }
        this.cachedGarageFields.removeAll(arenaId)
    }

    fun initGarageFields(gameGarageFields: MutableList<GameGarageField>) {
        transaction {
            for (gameGarageField: GameGarageField in gameGarageFields) {
                GameGarageFieldDAO.insert { statement: InsertStatement<Number> ->
                    statement[id] = gameGarageField.id
                    statement[arenaId] = gameGarageField.arenaId
                    statement[teamName] = gameGarageField.teamName
                    statement[worldName] = gameGarageField.world.name
                    statement[x] = gameGarageField.x
                    statement[z] = gameGarageField.z
                }

                cachedGarageFields.put(gameGarageField.arenaId, gameGarageField)
            }
        }
    }

}