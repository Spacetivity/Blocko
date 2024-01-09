package net.spacetivity.ludo.arena.setup

import net.spacetivity.ludo.field.GameField

class GameArenaSetupData(val arenaId: String) {

    val gameFields: MutableList<GameField> = mutableListOf()

}