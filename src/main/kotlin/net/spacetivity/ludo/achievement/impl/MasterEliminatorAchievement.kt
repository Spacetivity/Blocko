package net.spacetivity.ludo.achievement.impl

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.ludo.achievement.container.Achievement
import net.spacetivity.ludo.achievement.container.ProgressRequirement
import net.spacetivity.ludo.extensions.toStatsPlayerInstance
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.stats.StatsPlayer

class MasterEliminatorAchievement(translationKey: String) : Achievement(translationKey, 150, listOf(MasterEliminatorRequirement(translationKey, 2)))

class MasterEliminatorRequirement(override val translationKey: String, override val neededCount: Int) : ProgressRequirement<Int> {

    override fun getPlaceholders(gamePlayer: GamePlayer): List<TagResolver> {
        val statsPlayer: StatsPlayer = gamePlayer.toStatsPlayerInstance() ?: return listOf()
        return listOf(
            Placeholder.parsed("amount", this.neededCount.toString()),
            Placeholder.parsed("progress", getProgress(statsPlayer.eliminatedOpponents).toString())
        )
    }

    override fun isCompletedBy(gamePlayer: GamePlayer): Boolean {
        val statsPlayer: StatsPlayer = gamePlayer.toStatsPlayerInstance() ?: return false
        return statsPlayer.eliminatedOpponents == neededCount
    }

}
