package net.spacetivity.blocko.entity

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import net.spacetivity.blocko.team.GameTeamLocation
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.*
import kotlin.collections.set

class GameEntityHandler {

    val gameEntities: Multimap<String, GameEntity> = ArrayListMultimap.create()
    val unlockedGameEntityTypes: Multimap<UUID, GameEntityType> = ArrayListMultimap.create()
    val gameEntityHistories: MutableMap<UUID, GameEntityHistory> = mutableMapOf()

    fun getEntitiesFromTeam(arenaId: String, teamName: String): List<GameEntity> {
        return this.gameEntities.get(arenaId).filter { it.teamName.equals(teamName, true) }
    }

    fun clearEntitiesForTeam(arenaId: String, teamName: String) {
        getEntitiesFromTeam(arenaId, teamName).forEach { it.despawn() }
    }

    fun clearEntitiesFromArena(arenaId: String) {
        val entities: MutableCollection<GameEntity> = this.gameEntities.get(arenaId).toMutableList()
        entities.forEach { it.despawn() }
        this.gameEntities.removeAll(arenaId)
    }

    fun spawnEntity(gameTeamLocation: GameTeamLocation, type: GameEntityType) {
        val entityId: Int = this.gameEntities[gameTeamLocation.arenaId].filter { it.teamName == gameTeamLocation.teamName }.size

        val entityLocation = gameTeamLocation.getWorldPosition().clone()
        entityLocation.pitch = 0.0F

        GameEntity(gameTeamLocation.arenaId, gameTeamLocation.teamName, type, entityId).spawn(entityLocation)

        gameTeamLocation.isTaken = true
    }

    fun getSelectedEntityType(uuid: UUID): GameEntityType {
        return this.gameEntityHistories[uuid]?.selectedEntityType ?: GameEntityType.VILLAGER
    }

    fun setSelectedEntityType(uuid: UUID, gameEntityType: GameEntityType) {
        if (getSelectedEntityType(uuid) == gameEntityType) return

        this.gameEntityHistories[uuid]?.selectedEntityType = gameEntityType

        transaction {
            val resultRow: ResultRow? = GameEntityHistoryDAO.selectAll().where { GameEntityHistoryDAO.uuid eq uuid.toString() }.firstOrNull()

            if (resultRow == null) {
                GameEntityHistoryDAO.insert {
                    it[this.uuid] = uuid.toString()
                    it[this.selectedEntityType] = gameEntityType
                }
            } else {
                GameEntityHistoryDAO.update({ GameEntityHistoryDAO.uuid eq uuid.toString() }) {
                    it[this.selectedEntityType] = gameEntityType
                }
            }
        }
    }

    fun loadGameEntityHistory(uuid: UUID) {
        if (this.gameEntityHistories.containsKey(uuid)) return

        transaction {
            for (resultRow: ResultRow in GameEntityHistoryDAO.selectAll().where { GameEntityHistoryDAO.uuid eq uuid.toString() }.toMutableList()) {
                val gameEntityHistory = GameEntityHistory(UUID.fromString(resultRow[GameEntityHistoryDAO.uuid]), resultRow[GameEntityHistoryDAO.selectedEntityType])
                gameEntityHistories[uuid] = gameEntityHistory
            }
        }
    }

    fun unloadGameEntityHistory(uuid: UUID) {
        this.gameEntityHistories.remove(uuid)
    }

    fun hasUnlockedAllEntityTypes(uuid: UUID): Boolean {
        return this.unlockedGameEntityTypes.get(uuid).size == GameEntityType.entries.size
    }

    fun hasUnlockedEntityType(uuid: UUID, entityType: GameEntityType): Boolean {
        return this.unlockedGameEntityTypes.containsEntry(uuid, entityType)
    }

    fun getUnlockedEntityTypes(uuid: UUID): List<GameEntityType> {
        return this.unlockedGameEntityTypes.get(uuid).toList()
    }

    fun unlockEntityType(uuid: UUID, entityType: GameEntityType) {
        if (hasUnlockedEntityType(uuid, entityType)) return
        unlockedGameEntityTypes.put(uuid, entityType)
        transaction {
            GameEntityTypeDAO.insert { statement: InsertStatement<Number> ->
                statement[GameEntityTypeDAO.uuid] = uuid.toString()
                statement[entityTypeName] = entityType.name
            }
        }
    }

    fun loadUnlockedEntityTypes(uuid: UUID) {
        transaction {
            val results: MutableList<ResultRow> = GameEntityTypeDAO.selectAll().where { GameEntityTypeDAO.uuid eq uuid.toString() }.toMutableList()

            if (results.isEmpty()) {
                unlockEntityType(uuid, GameEntityType.VILLAGER)
                return@transaction
            }

            for (resultRow: ResultRow in results) {
                val entityTypeName: String = resultRow[GameEntityTypeDAO.entityTypeName]
                val gameEntityType: GameEntityType = GameEntityType.entries.find { it.name == entityTypeName }
                    ?: return@transaction
                unlockedGameEntityTypes.put(uuid, gameEntityType)
            }
        }
    }

    fun unloadUnlockedEntityTypes(uuid: UUID) {
        this.unlockedGameEntityTypes.removeAll(uuid)
    }

}