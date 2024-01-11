package net.spacetivity.ludo.field

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import net.spacetivity.ludo.utils.PathFace
import org.bukkit.Bukkit
import org.bukkit.World
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.transaction

class GameFieldHandler {

    val cachedGameFields: Multimap<String, GameField> = ArrayListMultimap.create()

    init {
        transaction {
            for (resultRow: ResultRow in GameFieldDAO.selectAll().toMutableList()) {
                val id: Int = resultRow[GameFieldDAO.id]
                val arenaId: String = resultRow[GameFieldDAO.arenaId]
                val world: World = Bukkit.getWorld(resultRow[GameFieldDAO.worldName]) ?: continue
                val x: Double = resultRow[GameFieldDAO.x]
                val z: Double = resultRow[GameFieldDAO.z]
                val turnComponent: TurnComponent? = if (resultRow[GameFieldDAO.turnComponent].isNullOrEmpty()) {
                    null
                } else {
                    TurnComponent(PathFace.valueOf(resultRow[GameFieldDAO.turnComponent]!!))
                }

                cachedGameFields.put(arenaId, GameField(id, arenaId, world, x, z, turnComponent, false))
            }
        }
    }

    fun getField(arenaId: String, id: Int): GameField? {
        return this.cachedGameFields.get(arenaId).find { it.id == id }
    }

    fun updateFieldTurnComponent(arenaId: String, id: Int, turnComponent: TurnComponent) {
        transaction {
            GameFieldDAO.update({ (GameFieldDAO.arenaId eq arenaId) and (GameFieldDAO.id eq id) }) { statement: UpdateStatement ->
                statement[GameFieldDAO.turnComponent] = turnComponent.facing.name
            }
        }
    }

    fun deleteFields(arenaId: String) {
        transaction { GameFieldDAO.deleteWhere { GameFieldDAO.arenaId eq arenaId } }
        this.cachedGameFields.removeAll(arenaId)
    }

    fun initFields(gameFields: MutableList<GameField>) {
        transaction {
            for (gameField: GameField in gameFields) {
                GameFieldDAO.insert { statement: InsertStatement<Number> ->
                    statement[id] = gameField.id
                    statement[arenaId] = gameField.arenaId
                    statement[worldName] = gameField.world.name
                    statement[x] = gameField.x
                    statement[z] = gameField.z
                    statement[turnComponent] = null
                }

                cachedGameFields.put(gameField.arenaId, gameField)
            }
        }
    }

}