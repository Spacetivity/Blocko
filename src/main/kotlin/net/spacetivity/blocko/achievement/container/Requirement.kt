package net.spacetivity.blocko.achievement.container

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.player.GamePlayer

interface Requirement {

    val translationKey: String

    fun getPlaceholders(gamePlayer: GamePlayer): List<TagResolver>

    fun isCompletedBy(gamePlayer: GamePlayer): Boolean

    fun getExplanationLine(gamePlayer: GamePlayer): Component {
        val translation = Blocko.instance.translationHandler.getSelectedTranslation()
        val key = Blocko.instance.getAchievementKey(false, this.translationKey)

        return translation.displayName(key, *getPlaceholders(gamePlayer).toTypedArray())
    }

}