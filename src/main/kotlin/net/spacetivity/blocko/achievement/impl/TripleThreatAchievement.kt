package net.spacetivity.blocko.achievement.impl

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.achievement.container.Achievement
import net.spacetivity.blocko.achievement.container.Requirement
import net.spacetivity.blocko.player.GamePlayer

class TripleThreatAchievement(translationKey: String) : Achievement(translationKey, 250, listOf(TripleThreatRequirement(translationKey)))

class TripleThreatRequirement(override val translationKey: String) : Requirement {
    override fun getPlaceholders(gamePlayer: GamePlayer): List<TagResolver> = emptyList()

    override fun isCompletedBy(gamePlayer: GamePlayer): Boolean {
        return gamePlayer.matchStats.eliminations >= 3
    }
}

