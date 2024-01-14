package net.spacetivity.ludo.arena.setup

import net.spacetivity.ludo.field.GameField
import net.spacetivity.ludo.garageField.GameGarageField
import net.spacetivity.ludo.team.GameTeam
import net.spacetivity.ludo.team.GameTeamLocation

class GameArenaSetupData(val arenaId: String) {

    val gameFields: MutableList<GameField> = mutableListOf()
    val gameGarageFields: MutableList<GameGarageField> = mutableListOf()
    val gameTeamLocations: MutableList<GameTeamLocation> = mutableListOf()

    val gameTeams: MutableList<GameTeam> = mutableListOf()

}