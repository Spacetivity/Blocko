package net.spacetivity.blocko.achievement.container

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.translation.Translation

interface Requirement {

    val translationKey: String

    fun getPlaceholders(gamePlayer: GamePlayer): List<TagResolver>

    fun isCompletedBy(gamePlayer: GamePlayer): Boolean

    fun getExplanationLine(gamePlayer: GamePlayer): Component {
        val translation: Translation = BlockoGame.instance.translationHandler.getSelectedTranslation()
        val key: String = BlockoGame.instance.getAchievementKey(false, this.translationKey)

        return translation.validateItemName(key, *getPlaceholders(gamePlayer).toTypedArray())
    }

}