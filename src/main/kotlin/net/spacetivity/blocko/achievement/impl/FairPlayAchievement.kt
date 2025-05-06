package net.spacetivity.blocko.achievement.impl

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.achievement.container.Achievement
import net.spacetivity.blocko.achievement.container.Requirement
import net.spacetivity.blocko.player.GamePlayer

class FairPlayAchievement(translationKey: String) : Achievement(translationKey, 10, listOf(FairPlayRequirement(translationKey)))

class FairPlayRequirement(override val translationKey: String) : Requirement {
    override fun getPlaceholders(gamePlayer: GamePlayer): List<TagResolver> = emptyList()

    override fun isCompletedBy(gamePlayer: GamePlayer): Boolean {
        val gameArena = Blocko.instance.arenaHandler.getArena(gamePlayer.arenaId) ?: return false
        return gameArena.phase.isEnding()
    }
}