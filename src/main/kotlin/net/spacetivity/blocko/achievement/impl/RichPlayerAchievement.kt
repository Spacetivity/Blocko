package net.spacetivity.blocko.achievement.impl

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.achievement.container.Achievement
import net.spacetivity.blocko.achievement.container.ProgressRequirement
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.stats.toStatsPlayerInstance

class RichPlayerAchievement(translationKey: String) : Achievement(translationKey, 2000, listOf(RichPlayerRequirement(translationKey, 10000)))

class RichPlayerRequirement(override val translationKey: String, override val neededCount: Int) : ProgressRequirement<Int> {

    override fun getPlaceholders(gamePlayer: GamePlayer): List<TagResolver> {
        val statsPlayer = gamePlayer.toStatsPlayerInstance() ?: return listOf()
        return listOf(
            Placeholder.parsed("current_amount", statsPlayer.coins.toString()),
            Placeholder.parsed("amount", this.neededCount.toString()),
            Placeholder.parsed("progress", getProgress(statsPlayer.coins).toString().split(".")[0])
        )
    }

    override fun isCompletedBy(gamePlayer: GamePlayer): Boolean {
        val statsPlayer = gamePlayer.toStatsPlayerInstance() ?: return false
        return statsPlayer.coins >= this.neededCount
    }

}

