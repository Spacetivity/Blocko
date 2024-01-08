package net.spacetivity.ludo.arena.setup

import net.spacetivity.ludo.board.GameField

class GameArenaSetupData(val arenaId: String) {

    val gameFields: MutableList<GameField> = mutableListOf()

}