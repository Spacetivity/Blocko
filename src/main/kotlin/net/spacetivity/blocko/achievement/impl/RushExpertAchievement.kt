package net.spacetivity.blocko.achievement.impl

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.achievement.container.Achievement
import net.spacetivity.blocko.achievement.container.Requirement
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.phase.impl.IngamePhase
import net.spacetivity.blocko.player.GamePlayer
import java.time.Duration

class RushExpertAchievement(translationKey: String) : Achievement(translationKey, 250, listOf(RushExpertRequirement(translationKey)))

class RushExpertRequirement(override val translationKey: String) : Requirement {
    override fun getPlaceholders(gamePlayer: GamePlayer): List<TagResolver> = emptyList()

    override fun isCompletedBy(gamePlayer: GamePlayer): Boolean {
        val gameArena: GameArena = BlockoGame.instance.gameArenaHandler.getArena(gamePlayer.arenaId) ?: return false
        if (!gameArena.phase.isIngame()) return false

        val ingamePhase: IngamePhase = gameArena.phase as IngamePhase

        val matchStartTime: Long = ingamePhase.matchStartTime ?: return false
        val matchDurationInMillis: Long = System.currentTimeMillis() - matchStartTime

        val fifteenMinutesInMillis: Long = Duration.ofMinutes(15).toMillis()

        return (matchDurationInMillis <= fifteenMinutesInMillis)
    }

}