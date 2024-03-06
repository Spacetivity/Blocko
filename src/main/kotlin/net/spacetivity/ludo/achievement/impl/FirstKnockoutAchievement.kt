package net.spacetivity.ludo.achievement.impl

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.ludo.achievement.container.Achievement
import net.spacetivity.ludo.achievement.container.Requirement
import net.spacetivity.ludo.extensions.toStatsPlayerInstance
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.stats.StatsPlayer

class FirstKnockoutAchievement(translationKey: String) : Achievement(translationKey, 0, listOf(FirstKnockoutRequirement(translationKey)))

class FirstKnockoutRequirement(override val translationKey: String) : Requirement {
    override fun getPlaceholders(gamePlayer: GamePlayer): List<TagResolver> = emptyList()

    override fun isCompletedBy(gamePlayer: GamePlayer): Boolean {
        val statsPlayer: StatsPlayer = gamePlayer.toStatsPlayerInstance() ?: return false
        return statsPlayer.knockedOutByOpponents == 0
    }
}