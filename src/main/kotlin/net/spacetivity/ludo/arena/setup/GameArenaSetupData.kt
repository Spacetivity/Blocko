package net.spacetivity.ludo.arena.setup

import net.spacetivity.ludo.field.GameField
import net.spacetivity.ludo.team.GameTeam
import net.spacetivity.ludo.team.GameTeamLocation

class GameArenaSetupData(val arenaId: String, val setupTool: GameArenaSetupTool) {

    val gameFields: MutableList<GameField> = mutableListOf()
    val gameTeamLocations: MutableList<GameTeamLocation> = mutableListOf()
    val gameTeams: MutableList<GameTeam> = mutableListOf()

}