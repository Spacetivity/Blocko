package net.spacetivity.blocko.achievement.impl

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.achievement.container.Achievement
import net.spacetivity.blocko.achievement.container.ProgressRequirement
import net.spacetivity.blocko.player.GamePlayer

class EntityCollectorAchievement(translationKey: String) : Achievement(translationKey, 10000, listOf(EntityCollectorRequirement(translationKey, 55)))

class EntityCollectorRequirement(override val translationKey: String, override val neededCount: Int) : ProgressRequirement<Int> {

    override fun getPlaceholders(gamePlayer: GamePlayer): List<TagResolver> {
        val size: Int = BlockoGame.instance.gameEntityHandler.getUnlockedEntityTypes(gamePlayer.uuid).size

        return listOf(
            Placeholder.parsed("current_amount", size.toString()),
            Placeholder.parsed("amount", this.neededCount.toString()),
            Placeholder.parsed("progress", getProgress(size).toString().split(".")[0])
        )
    }

    override fun isCompletedBy(gamePlayer: GamePlayer): Boolean {
        return BlockoGame.instance.gameEntityHandler.hasUnlockedAllEntityTypes(gamePlayer.uuid)
    }

}
