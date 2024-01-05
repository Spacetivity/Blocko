package net.spacetivity.ludo.board

class GameBoard(
    val name: String,
    val maxTeams: Int,
    val enableSafetyFields: Boolean,
    val fields: MutableSet<GameField>
) {

    fun reset() {
        TODO("Remove players, teleport entities back to their spawns")
    }

    fun getField(id: Int): GameField? = this.fields.find { it.id == id }

}