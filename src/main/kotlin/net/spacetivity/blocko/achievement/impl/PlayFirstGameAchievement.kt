package net.spacetivity.blocko.achievement.impl

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.achievement.container.Achievement
import net.spacetivity.blocko.achievement.container.Requirement
import net.spacetivity.blocko.extensions.toStatsPlayerInstance
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.stats.StatsPlayer

class PlayFirstGameAchievement(translationKey: String) : Achievement(translationKey, 15, listOf(PlayFirstGameRequirement(translationKey)))

class PlayFirstGameRequirement(override val translationKey: String) : Requirement {
    override fun getPlaceholders(gamePlayer: GamePlayer): List<TagResolver> = emptyList()

    override fun isCompletedBy(gamePlayer: GamePlayer): Boolean {
        val statsPlayer: StatsPlayer = gamePlayer.toStatsPlayerInstance() ?: return false
        return statsPlayer.playedGames == 0
    }
}