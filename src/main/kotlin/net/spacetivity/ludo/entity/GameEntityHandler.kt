package net.spacetivity.ludo.entity

import java.util.*

class GameEntityHandler {

    val gameEntities: MutableList<GameEntity> = mutableListOf()

    fun getEntity(uuid: UUID): GameEntity? {
        return this.gameEntities.find { it.livingEntity?.uniqueId == uuid }
    }

    fun getEntitiesFromTeam(teamName: String): List<GameEntity> {
        return this.gameEntities.filter { it.teamName.equals(teamName, true) }
    }

}