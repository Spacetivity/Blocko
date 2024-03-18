package net.spacetivity.blocko.stats

import net.spacetivity.blocko.BlockoGame
import java.util.*

data class StatsPlayer(val uuid: UUID, var eliminatedOpponents: Int, var knockedOutByOpponents: Int, var playedGames: Int, var coins: Int) {

    fun updateDbEntry() {
        BlockoGame.instance.statsPlayerHandler.updateStatsPlayer(this)
    }

    fun update(type: StatsType, operation: UpdateOperation, newValue: Int) {
        when (type) {
            StatsType.ELIMINATED_OPPONENTS -> if (operation == UpdateOperation.INCREASE) this.eliminatedOpponents += newValue else this.eliminatedOpponents -= newValue
            StatsType.KNOCKED_OUT_BY_OPPONENTS -> if (operation == UpdateOperation.INCREASE) this.knockedOutByOpponents += newValue else this.knockedOutByOpponents -= newValue
            StatsType.COINS -> if (operation == UpdateOperation.INCREASE) this.coins += newValue else this.coins -= newValue
            StatsType.PLAYED_GAMES -> if (operation == UpdateOperation.INCREASE) this.playedGames += newValue else this.playedGames -= newValue
        }
    }

    fun getStatsValue(type: StatsType): Int = when (type) {
        StatsType.ELIMINATED_OPPONENTS -> this.eliminatedOpponents
        StatsType.KNOCKED_OUT_BY_OPPONENTS -> this.knockedOutByOpponents
        StatsType.COINS -> this.coins
        StatsType.PLAYED_GAMES -> this.playedGames
    }

}

enum class StatsType(val nameKey: String) {
    ELIMINATED_OPPONENTS("blocko.stats.type.eliminations"),
    KNOCKED_OUT_BY_OPPONENTS("blocko.stats.type.knocked_out_by_opponents"),
    COINS("blocko.stats.type.coins"),
    PLAYED_GAMES("blocko.stats.type.played_games");
}

enum class UpdateOperation {
    INCREASE,
    DECREASE;
}