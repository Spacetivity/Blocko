package net.spacetivity.blocko.achievement.impl

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.achievement.container.Achievement
import net.spacetivity.blocko.achievement.container.Requirement
import net.spacetivity.blocko.player.GamePlayer

class ComebackKingAchievement(translationKey: String) : Achievement(translationKey, 350, listOf(ComebackKingRequirement(translationKey)))

class ComebackKingRequirement(override val translationKey: String) : Requirement {
    override fun getPlaceholders(gamePlayer: GamePlayer): List<TagResolver> = emptyList()

    override fun isCompletedBy(gamePlayer: GamePlayer): Boolean {
        return gamePlayer.matchStats.knockedOutByOpponent > 0 && gamePlayer.hasSavedAllEntities()
    }
}

