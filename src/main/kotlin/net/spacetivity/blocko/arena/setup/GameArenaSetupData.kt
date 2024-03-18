package net.spacetivity.blocko.arena.setup

import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.team.GameTeam
import net.spacetivity.blocko.team.GameTeamLocation
import java.time.Duration

class GameArenaSetupData(val arenaId: String, val setupTool: GameArenaSetupTool) {

    val timeoutTimestamp: Long = System.currentTimeMillis() + Duration.ofMinutes(10).toMillis()

    val gameFields: MutableList<GameField> = mutableListOf()
    val gameTeamLocations: MutableList<GameTeamLocation> = mutableListOf()
    val gameTeams: MutableList<GameTeam> = mutableListOf()

}