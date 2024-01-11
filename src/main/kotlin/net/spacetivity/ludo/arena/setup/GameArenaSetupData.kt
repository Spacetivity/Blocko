package net.spacetivity.ludo.arena.setup

import net.spacetivity.ludo.field.GameField
import net.spacetivity.ludo.team.GameTeamSpawn

class GameArenaSetupData(val arenaId: String) {

    val gameFields: MutableList<GameField> = mutableListOf()
    val gameTeamSpawns: MutableList<GameTeamSpawn> = mutableListOf()

}