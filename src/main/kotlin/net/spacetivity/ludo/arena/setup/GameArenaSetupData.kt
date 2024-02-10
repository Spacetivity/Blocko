package net.spacetivity.ludo.arena.setup

import net.spacetivity.ludo.field.GameField
import net.spacetivity.ludo.team.GameTeam
import net.spacetivity.ludo.team.GameTeamLocation
import java.time.Duration

class GameArenaSetupData(val arenaId: String, val setupTool: GameArenaSetupTool) {

    val timeoutTimestamp: Long = System.currentTimeMillis() + Duration.ofMinutes(10).toMillis()

    val gameFields: MutableList<GameField> = mutableListOf()
    val gameTeamLocations: MutableList<GameTeamLocation> = mutableListOf()
    val gameTeams: MutableList<GameTeam> = mutableListOf()

}