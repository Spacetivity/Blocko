package net.spacetivity.ludo.field

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import net.spacetivity.ludo.LudoGame
import org.bukkit.Bukkit
import org.bukkit.World
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.transaction

class GameFieldHandler {

    private val cachedGameFields: Multimap<String, GameField> = ArrayListMultimap.create()

    init {
        transaction {
            for (resultRow: ResultRow in GameFieldDAO.selectAll().toMutableList()) {
                val arenaId: String = resultRow[GameFieldDAO.arenaId]
                val world: World = Bukkit.getWorld(resultRow[GameFieldDAO.worldName]) ?: continue
                val x: Double = resultRow[GameFieldDAO.x]
                val z: Double = resultRow[GameFieldDAO.z]
                val properties: GameFieldProperties = LudoGame.GSON.fromJson(resultRow[GameFieldDAO.properties], GameFieldProperties::class.java)
                val isGarageField: Boolean = resultRow[GameFieldDAO.isGarageField]

                cachedGameFields.put(arenaId, GameField(arenaId, world, x, z, properties, isGarageField, false))
            }
        }
    }

    fun getFirstFieldForTeam(arenaId: String, teamName: String): GameField? {
        return this.cachedGameFields[arenaId].find { it.properties.getFieldId(teamName) == 0 }
    }

    fun getLastFieldForTeam(arenaId: String, teamName: String): GameField? {
        val gameFieldsForTeam: MutableCollection<GameField> = this.cachedGameFields[arenaId]
        val validTeamFieldIds: MutableList<Int> = mutableListOf()

        for (gameField in gameFieldsForTeam) {
            val fieldId: Int = gameField.properties.getFieldId(teamName) ?: continue
            validTeamFieldIds.add(fieldId)
        }

        val highestTeamFieldId: Int = validTeamFieldIds.maxOrNull() ?: return null
        val lastGameField: GameField? = gameFieldsForTeam.find { it.properties.getFieldId(teamName) == highestTeamFieldId }

        return lastGameField
    }

    fun getFieldForTeam(arenaId: String, teamName: String, id: Int): GameField? {
        return this.cachedGameFields[arenaId].find { it.properties.getFieldId(teamName) == id }
    }

    fun getField(arenaId: String, x: Double, z: Double): GameField? {
        return this.cachedGameFields.get(arenaId).find { it.x == x && it.z == z }
    }

    fun deleteFields(arenaId: String) {
        transaction { GameFieldDAO.deleteWhere { GameFieldDAO.arenaId eq arenaId } }
        this.cachedGameFields.removeAll(arenaId)
    }

    fun initFields(gameFields: MutableList<GameField>) {
        transaction {
            for (gameField: GameField in gameFields) {
                GameFieldDAO.insert { statement: InsertStatement<Number> ->
                    statement[arenaId] = gameField.arenaId
                    statement[worldName] = gameField.world.name
                    statement[x] = gameField.x
                    statement[z] = gameField.z
                    statement[properties] = LudoGame.GSON.toJson(gameField.properties)
                    statement[isGarageField] = gameField.isGarageField
                }

                cachedGameFields.put(gameField.arenaId, gameField)
            }
        }
    }

}