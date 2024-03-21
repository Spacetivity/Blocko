package net.spacetivity.blocko.achievement.impl

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.achievement.container.Achievement
import net.spacetivity.blocko.achievement.container.ProgressRequirement
import net.spacetivity.blocko.extensions.toStatsPlayerInstance
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.stats.StatsPlayer

class WinMonsterAchievement(translationKey: String) : Achievement(translationKey, 165, listOf(WinMonsterRequirement(translationKey, 20)))

class WinMonsterRequirement(override val translationKey: String, override val neededCount: Int) : ProgressRequirement<Int> {

    override fun getPlaceholders(gamePlayer: GamePlayer): List<TagResolver> {
        val statsPlayer: StatsPlayer = gamePlayer.toStatsPlayerInstance() ?: return listOf()
        return listOf(
            Placeholder.parsed("current_amount", statsPlayer.eliminatedOpponents.toString()),
            Placeholder.parsed("amount", this.neededCount.toString()),
            Placeholder.parsed("progress", getProgress(statsPlayer.eliminatedOpponents).toString().split(".")[0])
        )
    }

    override fun isCompletedBy(gamePlayer: GamePlayer): Boolean {
        val statsPlayer: StatsPlayer = gamePlayer.toStatsPlayerInstance() ?: return false
        return statsPlayer.wonGames == 20
    }

}
