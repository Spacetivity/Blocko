package net.spacetivity.ludo.entity

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import java.util.*

class GameEntityHandler {

    val gameEntities: Multimap<String, GameEntity> = ArrayListMultimap.create()

    fun getEntity(arenaId: String, uuid: UUID): GameEntity? {
        return this.gameEntities.get(arenaId).find { it.livingEntity?.uniqueId == uuid }
    }

    fun getEntityAtField(arenaId: String, fieldId: Int): GameEntity? {
        return this.gameEntities.get(arenaId).find { it.currentFieldId == fieldId }
    }

    fun getEntitiesFromTeam(arenaId: String, teamName: String): List<GameEntity> {
        return this.gameEntities.get(arenaId).filter { it.teamName.equals(teamName, true) }
    }

    fun clearEntitiesFromArena(arenaId: String) {
        val entities: MutableCollection<GameEntity> = this.gameEntities.get(arenaId).toMutableList()
        entities.forEach { it.despawn() }
        this.gameEntities.removeAll(arenaId)
    }

}