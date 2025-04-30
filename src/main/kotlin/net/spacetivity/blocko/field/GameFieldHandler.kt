package net.spacetivity.blocko.field

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.id.ArenaId
import org.bukkit.Bukkit
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class GameFieldHandler {

    val cachedGameFields: Multimap<ArenaId, GameField> = ArrayListMultimap.create()

    init {
        transaction {
            for (resultRow in GameFieldDAO.selectAll().toMutableList()) {
                val arenaId = resultRow[GameFieldDAO.arenaId]
                val world = Bukkit.getWorld(resultRow[GameFieldDAO.worldName]) ?: continue
                val x = resultRow[GameFieldDAO.x]
                val z = resultRow[GameFieldDAO.z]
                val properties = BlockoGame.GSON.fromJson(resultRow[GameFieldDAO.properties], GameFieldProperties::class.java)
                val isGarageField = resultRow[GameFieldDAO.isGarageField]

                cachedGameFields.put(arenaId, GameField(arenaId, world, x, z, properties, isGarageField, false))
            }
        }
    }

    fun getFirstFieldForTeam(arenaId: ArenaId, teamName: String): GameField? {
        return this.cachedGameFields[arenaId].find { it.properties.getFieldId(teamName) == 0 }
    }

    fun getLastFieldForTeam(arenaId: ArenaId, teamName: String): GameField? {
        val gameFieldsForTeam = this.cachedGameFields[arenaId]
        val validTeamFieldIds = mutableListOf<Int>()

        for (gameField in gameFieldsForTeam) {
            val fieldId = gameField.properties.getFieldId(teamName) ?: continue
            validTeamFieldIds.add(fieldId)
        }

        val highestTeamFieldId = validTeamFieldIds.maxOrNull() ?: return null
        val lastGameField = gameFieldsForTeam.find { it.properties.getFieldId(teamName) == highestTeamFieldId }

        return lastGameField
    }

    fun getFieldForTeam(arenaId: ArenaId, teamName: String, id: Int): GameField? {
        return this.cachedGameFields[arenaId].find { it.properties.getFieldId(teamName) == id }
    }

    fun getField(arenaId: ArenaId, x: Int, z: Int): GameField? {
        return this.cachedGameFields.get(arenaId).find { it.x == x && it.z == z }
    }

    fun deleteFields(arenaId: ArenaId) {
        transaction { GameFieldDAO.deleteWhere { GameFieldDAO.arenaId eq arenaId } }
        this.cachedGameFields.removeAll(arenaId)
    }

    fun initFields(gameFields: MutableList<GameField>) {
        transaction {
            for (gameField in gameFields) {
                GameFieldDAO.insert { statement ->
                    statement[arenaId] = gameField.arenaId
                    statement[worldName] = gameField.world.name
                    statement[x] = gameField.x
                    statement[z] = gameField.z
                    statement[properties] = BlockoGame.GSON.toJson(gameField.properties)
                    statement[isGarageField] = gameField.isGarageField
                }

                cachedGameFields.put(gameField.arenaId, gameField)
            }
        }
    }

}