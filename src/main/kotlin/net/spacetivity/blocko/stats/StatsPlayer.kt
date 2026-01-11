package net.spacetivity.blocko.stats

import net.spacetivity.blocko.Blocko
import java.util.*

data class StatsPlayer(val uuid: UUID, var eliminatedOpponents: Int, var knockedOutByOpponents: Int, var playedGames: Int, var wonGames: Int, var coins: Int) {

    fun updateDbEntry() = Blocko.instance.statsPlayerHandler.updateStatsPlayer(this)

    fun update(type: StatsType, increase: Boolean, newValue: Int) {
        when (type) {
            StatsType.ELIMINATED_OPPONENTS -> if (increase) this.eliminatedOpponents += newValue else this.eliminatedOpponents -= newValue
            StatsType.KNOCKED_OUT_BY_OPPONENTS -> if (increase) this.knockedOutByOpponents += newValue else this.knockedOutByOpponents -= newValue
            StatsType.COINS -> if (increase) this.coins += newValue else this.coins -= newValue
            StatsType.PLAYED_GAMES -> if (increase) this.playedGames += newValue else this.playedGames -= newValue
            StatsType.WON_GAMES -> if (increase) this.wonGames += newValue else this.wonGames -= newValue
        }
    }

    fun getStatsValue(type: StatsType): Int = when (type) {
        StatsType.ELIMINATED_OPPONENTS -> this.eliminatedOpponents
        StatsType.KNOCKED_OUT_BY_OPPONENTS -> this.knockedOutByOpponents
        StatsType.COINS -> this.coins
        StatsType.PLAYED_GAMES -> this.playedGames
        StatsType.WON_GAMES -> this.wonGames
    }

}

