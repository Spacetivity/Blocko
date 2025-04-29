package net.spacetivity.blocko.achievement.impl

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.achievement.container.Achievement
import net.spacetivity.blocko.achievement.container.Requirement
import net.spacetivity.blocko.phase.impl.IngamePhase
import net.spacetivity.blocko.player.GamePlayer
import java.time.Duration

class RushExpertAchievement(translationKey: String) : Achievement(translationKey, 250, listOf(RushExpertRequirement(translationKey)))

class RushExpertRequirement(override val translationKey: String) : Requirement {
    override fun getPlaceholders(gamePlayer: GamePlayer): List<TagResolver> = emptyList()

    override fun isCompletedBy(gamePlayer: GamePlayer): Boolean {
        val gameArena = BlockoGame.instance.arenaHandler.getArena(gamePlayer.arenaId) ?: return false
        if (!gameArena.phase.isIngame()) return false

        val ingamePhase = gameArena.phase as IngamePhase

        val matchStartTime = ingamePhase.matchStartTime ?: return false
        val matchDurationInMillis = System.currentTimeMillis() - matchStartTime

        val fifteenMinutesInMillis = Duration.ofMinutes(15).toMillis()

        return (matchDurationInMillis <= fifteenMinutesInMillis)
    }

}