package net.spacetivity.blocko.team

enum class GameTeamOptions(val id: Int, val playerCount: Int) {

    FOUR_BY_ONE(0, 4),
    THREE_BY_ONE(1, 3),
    TWO_BY_ONE(2, 2);

    fun getDisplayString(): String = "${this.playerCount}x1"


}