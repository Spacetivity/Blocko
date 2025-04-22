package net.spacetivity.blocko.arena.setup

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.team.GameTeam
import net.spacetivity.blocko.team.GameTeamLocation
import org.bukkit.Location
import java.time.Duration

class GameArenaSetupData(val arenaId: String, val setupTool: GameArenaSetupTool) {

    val timeoutTimestamp: Long
        get() = if (BlockoGame.instance.setupConfigFile.setupSessionEndless)
            -1
        else
            System.currentTimeMillis() + Duration.ofMinutes(BlockoGame.instance.setupConfigFile.setupSessionTimeoutMinutes.toLong()).toMillis()

    val gameFields: MutableList<GameField> = mutableListOf()
    val gameTeamLocations: MutableList<GameTeamLocation> = mutableListOf()
    val gameTeams: MutableList<GameTeam> = mutableListOf()

    var corner1: Location? = null
    var corner2: Location? = null
    var missingResults: MutableMap<ScannerResult, Int> = mutableMapOf()

    fun areCornersSet(): Boolean = this.corner1 != null && this.corner2 != null

}