package net.spacetivity.ludo.field

import net.spacetivity.ludo.utils.PathFace

data class GameFieldProperties(val teamFieldIds: MutableMap<String, Int>, var garageForTeam: String?, var teamEntrance: String?, var rotation: PathFace?) {

    fun getFieldId(teamName: String): Int? {
        return this.teamFieldIds[teamName]
    }

    fun setFieldId(teamName: String, id: Int) {
        this.teamFieldIds[teamName] = id
    }

}