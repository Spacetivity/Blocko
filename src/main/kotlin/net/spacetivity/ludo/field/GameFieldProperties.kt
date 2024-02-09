package net.spacetivity.ludo.field

import net.spacetivity.ludo.entity.GameEntity
import net.spacetivity.ludo.team.GameTeam
import net.spacetivity.ludo.utils.PathFace

data class GameFieldProperties(val teamFieldIds: MutableMap<String, Int>, var teamEntrance: String?, var turnComponent: PathFace?) {

    fun getFieldId(gameTeam: GameTeam): Int? {
        return this.teamFieldIds[gameTeam.name]
    }

    fun setFieldId(teamName: String, id: Int) {
        this.teamFieldIds[teamName] = id
    }

    fun isTurnNeeded(entity: GameEntity): Boolean {
        return when {
            this.teamEntrance == null && this.turnComponent != null -> true
            this.teamEntrance != null && this.turnComponent != null && this.teamEntrance!! == entity.teamName -> true
            else -> false
        }
    }

}