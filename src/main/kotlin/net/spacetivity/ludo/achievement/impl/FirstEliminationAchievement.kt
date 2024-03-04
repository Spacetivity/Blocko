package net.spacetivity.ludo.achievement.impl

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.achievement.container.Achievement
import net.spacetivity.ludo.achievement.container.Requirement
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.stats.StatsPlayer

class FirstEliminationAchievement : Achievement("First Elimination", "Eliminate your first opponent!", 30, listOf(FirstEliminationRequirement()))

class FirstEliminationRequirement : Requirement {
    override fun isCompletedBy(gamePlayer: GamePlayer): Boolean {
        val statsPlayer: StatsPlayer = LudoGame.instance.statsPlayerHandler.getStatsPlayer(gamePlayer.uuid) ?: return false
        return statsPlayer.eliminatedOpponents == 0
    }
}