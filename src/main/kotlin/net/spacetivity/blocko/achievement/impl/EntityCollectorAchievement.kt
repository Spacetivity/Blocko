package net.spacetivity.blocko.achievement.impl

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.achievement.container.Achievement
import net.spacetivity.blocko.achievement.container.ProgressRequirement
import net.spacetivity.blocko.extensions.toStatsPlayerInstance
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.stats.StatsPlayer

class EntityCollectorAchievement(translationKey: String) : Achievement(translationKey, 10000, listOf(EntityCollectorRequirement(translationKey, 54)))

class EntityCollectorRequirement(override val translationKey: String, override val neededCount: Int) : ProgressRequirement<Int> {

    override fun getPlaceholders(gamePlayer: GamePlayer): List<TagResolver> {
        val statsPlayer: StatsPlayer = gamePlayer.toStatsPlayerInstance() ?: return listOf()
        return listOf(
            Placeholder.parsed("current_amount", statsPlayer.eliminatedOpponents.toString()),
            Placeholder.parsed("amount", this.neededCount.toString()),
            Placeholder.parsed("progress", getProgress(statsPlayer.eliminatedOpponents).toString().split(".")[0])
        )
    }

    override fun isCompletedBy(gamePlayer: GamePlayer): Boolean {
        return BlockoGame.instance.gameEntityHandler.hasUnlockedAllEntityTypes(gamePlayer.uuid)
    }

}
