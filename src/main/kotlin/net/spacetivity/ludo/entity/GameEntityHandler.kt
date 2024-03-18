package net.spacetivity.ludo.entity

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import net.spacetivity.ludo.team.GameTeamLocation
import org.bukkit.entity.EntityType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class GameEntityHandler {

    val gameEntities: Multimap<String, GameEntity> = ArrayListMultimap.create()
    val unlockedGameEntityTypes: Multimap<UUID, GameEntityType> = ArrayListMultimap.create()

    fun getEntitiesFromTeam(arenaId: String, teamName: String): List<GameEntity> {
        return this.gameEntities.get(arenaId).filter { it.teamName.equals(teamName, true) }
    }

    fun clearEntitiesFromArena(arenaId: String) {
        val entities: MutableCollection<GameEntity> = this.gameEntities.get(arenaId).toMutableList()
        entities.forEach { it.despawn() }
        this.gameEntities.removeAll(arenaId)
    }

    fun spawnEntity(gameTeamLocation: GameTeamLocation, type: EntityType) {
        val entityId: Int = this.gameEntities[gameTeamLocation.arenaId].filter { it.teamName == gameTeamLocation.teamName }.size
        GameEntity(gameTeamLocation.arenaId, gameTeamLocation.teamName, type, entityId).spawn(gameTeamLocation.getWorldPosition())
        gameTeamLocation.isTaken = true
    }

    fun hasUnlockedEntityType(uuid: UUID, entityType: GameEntityType): Boolean {
        return this.unlockedGameEntityTypes.containsEntry(uuid, entityType)
    }

    fun getUnlockedEntityTypes(uuid: UUID): List<GameEntityType> {
        return this.unlockedGameEntityTypes.get(uuid).toList()
    }

    fun unlockEntityType(uuid: UUID, entityType: GameEntityType) {
        if (hasUnlockedEntityType(uuid, entityType)) return
        transaction {
            GameEntityTypeDAO.insert { statement: InsertStatement<Number> ->
                statement[GameEntityTypeDAO.uuid] = uuid.toString()
                statement[entityTypeName] = entityType.name
            }
            unlockedGameEntityTypes.put(uuid, entityType)
        }
    }

    fun loadUnlockedEntityTypes(uuid: UUID) {
        transaction {
            for (resultRow: ResultRow in GameEntityTypeDAO.select() { GameEntityTypeDAO.uuid eq uuid.toString() }.toMutableList()) {
                val entityTypeName: String = resultRow[GameEntityTypeDAO.entityTypeName]
                val gameEntityType: GameEntityType = GameEntityType.entries.find { it.name == entityTypeName } ?: return@transaction
                unlockedGameEntityTypes.put(uuid, gameEntityType)
            }
        }
    }

    fun unloadUnlockedEntityTypes(uuid: UUID) {
        this.unlockedGameEntityTypes.removeAll(uuid)
    }

}