package net.spacetivity.ludo.achievement.container

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.translation.Translation

interface Requirement {

    val translationKey: String

    fun getPlaceholders(gamePlayer: GamePlayer): List<TagResolver>

    fun isCompletedBy(gamePlayer: GamePlayer): Boolean

    fun getExplanationLine(gamePlayer: GamePlayer, isNormalMessage: Boolean): Component {
        val translation: Translation = LudoGame.instance.translationHandler.getSelectedTranslation()
        return if (isNormalMessage) translation.validateItemName(LudoGame.instance.getAchievementKey(false, this.translationKey), *getPlaceholders(gamePlayer).toTypedArray()) else translation.validateLine(translationKey, *getPlaceholders(gamePlayer).toTypedArray())
    }

}