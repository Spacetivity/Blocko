package net.spacetivity.ludo.board

class GameBoard(
    val arenaId: String,
    val name: String,
) {

    private val gameFields: MutableSet<GameField> = mutableSetOf()

    fun getField(id: Int): GameField? = this.gameFields.find { it.id == id }

}