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

    fun getExplanationLine(gamePlayer: GamePlayer): Component {
        val translation: Translation = LudoGame.instance.translationHandler.getSelectedTranslation()
        val key: String = LudoGame.instance.getAchievementKey(false, this.translationKey)

        return translation.validateItemName(key, *getPlaceholders(gamePlayer).toTypedArray())
    }

}