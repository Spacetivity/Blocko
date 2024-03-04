package net.spacetivity.ludo.stats

import net.spacetivity.ludo.LudoGame
import java.util.*

data class StatsPlayer(val uuid: UUID, var eliminatedOpponents: Int, var knockedOutByOpponents: Int, var playedGames: Int, var coins: Int) {

    fun updateDbEntry() {
        LudoGame.instance.statsPlayerHandler.updateStatsPlayer(this)
    }

    fun update(type: UpdateType, operator: UpdateOperator, newValue: Int) {
        when (type) {
            UpdateType.ELIMINATED_OPPONENTS -> if (operator == UpdateOperator.INCREASE) this.eliminatedOpponents += newValue else this.eliminatedOpponents -= newValue
            UpdateType.KNOCKED_OUT_BY_OPPONENTS -> if (operator == UpdateOperator.INCREASE) this.knockedOutByOpponents += newValue else this.knockedOutByOpponents -= newValue
            UpdateType.PLAYED_GAMES -> if (operator == UpdateOperator.INCREASE) this.playedGames += newValue else this.playedGames -= newValue
            UpdateType.COINS -> if (operator == UpdateOperator.INCREASE) this.coins += newValue else this.coins -= newValue
        }
    }

}

enum class UpdateType {
    ELIMINATED_OPPONENTS,
    KNOCKED_OUT_BY_OPPONENTS,
    PLAYED_GAMES,
    COINS;
}

enum class UpdateOperator {
    INCREASE,
    DECREASE;
}