package net.spacetivity.blocko.achievement.impl

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.achievement.container.Achievement
import net.spacetivity.blocko.achievement.container.Requirement
import net.spacetivity.blocko.phase.impl.IngamePhase
import net.spacetivity.blocko.player.GamePlayer
import java.time.Duration

class SpeedDemonAchievement(translationKey: String) : Achievement(translationKey, 400, listOf(SpeedDemonRequirement(translationKey)))

class SpeedDemonRequirement(override val translationKey: String) : Requirement {
    override fun getPlaceholders(gamePlayer: GamePlayer): List<TagResolver> = emptyList()

    override fun isCompletedBy(gamePlayer: GamePlayer): Boolean {
        val gameArena = Blocko.instance.arenaHandler.getArena(gamePlayer.arenaId) ?: return false
        if (!gameArena.phase.isIngame()) return false

        val ingamePhase = gameArena.phase as IngamePhase

        val matchStartTime = ingamePhase.matchStartTime ?: return false
        val matchDurationInMillis = System.currentTimeMillis() - matchStartTime

        val tenMinutesInMillis = Duration.ofMinutes(10).toMillis()

        return (matchDurationInMillis <= tenMinutesInMillis) && gamePlayer.hasSavedAllEntities()
    }
}

