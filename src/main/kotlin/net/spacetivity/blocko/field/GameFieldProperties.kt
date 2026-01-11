package net.spacetivity.blocko.field

data class GameFieldProperties(val teamPathIds: MutableMap<String, Int>, var garageForTeam: String?, var teamEntrance: String?, var rotation: GameFieldRotation?) {

    fun getTeamPathId(teamName: String): Int? {
        return this.teamPathIds[teamName]
    }

    fun setTeamPathId(teamName: String, id: Int) {
        this.teamPathIds[teamName] = id
    }

    fun removeTeamPathId(teamName: String) {
        this.teamPathIds.remove(teamName)
    }

}