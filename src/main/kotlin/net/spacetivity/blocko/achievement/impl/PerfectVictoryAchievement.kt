package net.spacetivity.blocko.achievement.impl

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.achievement.container.Achievement
import net.spacetivity.blocko.achievement.container.Requirement
import net.spacetivity.blocko.player.GamePlayer

class PerfectVictoryAchievement(translationKey: String) : Achievement(translationKey, 300, listOf(PerfectVictoryRequirement(translationKey)))

class PerfectVictoryRequirement(override val translationKey: String) : Requirement {
    override fun getPlaceholders(gamePlayer: GamePlayer): List<TagResolver> = emptyList()

    override fun isCompletedBy(gamePlayer: GamePlayer): Boolean {
        return gamePlayer.matchStats.eliminations == 0 && gamePlayer.hasSavedAllEntities()
    }
}

