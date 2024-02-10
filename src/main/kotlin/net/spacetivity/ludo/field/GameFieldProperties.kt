package net.spacetivity.ludo.field

import net.spacetivity.ludo.utils.PathFace

data class GameFieldProperties(val teamFieldIds: MutableMap<String, Int>, var garageForTeam: String?, var turnComponent: PathFace?) {

    fun getFieldId(teamName: String): Int? {
        return this.teamFieldIds[teamName]
    }

    fun setFieldId(teamName: String, id: Int) {
        this.teamFieldIds[teamName] = id
    }

    fun isTurnNeeded(): Boolean {
        return this.turnComponent != null
    }

}