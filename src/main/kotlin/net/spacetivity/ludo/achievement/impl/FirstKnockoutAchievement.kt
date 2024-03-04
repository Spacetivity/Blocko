package net.spacetivity.ludo.achievement.impl

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.achievement.container.Achievement
import net.spacetivity.ludo.achievement.container.Requirement
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.stats.StatsPlayer

class FirstKnockoutAchievement : Achievement("First Knockout", "Get knocked out by an opponent!", 0, listOf(FirstKnockoutRequirement()))

class FirstKnockoutRequirement : Requirement {
    override fun isCompletedBy(gamePlayer: GamePlayer): Boolean {
        val statsPlayer: StatsPlayer = LudoGame.instance.statsPlayerHandler.getStatsPlayer(gamePlayer.uuid) ?: return false
        return statsPlayer.knockedOutByOpponents == 0
    }
}