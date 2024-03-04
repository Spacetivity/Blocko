package net.spacetivity.ludo.achievement.impl

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.achievement.container.Achievement
import net.spacetivity.ludo.achievement.container.Requirement
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.stats.StatsPlayer

class PlayFirstGameAchievement : Achievement("FirstGame", "Play your first Blocko round!", listOf(PlayFirstGameRequirement()))

class PlayFirstGameRequirement : Requirement {
    override fun isCompletedBy(gamePlayer: GamePlayer): Boolean {
        val statsPlayer: StatsPlayer = LudoGame.instance.statsPlayerHandler.getCachedStatsPlayer(gamePlayer.uuid) ?: return false
        return statsPlayer.playedGames == 0
    }
}