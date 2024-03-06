package net.spacetivity.ludo.achievement.container

import net.kyori.adventure.text.Component
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.player.GamePlayer
import org.bukkit.Material

open class Achievement(val translationKey: String, val rewardedCoins: Int, private val requirements: List<Requirement>) {

    val name: String = LudoGame.instance.translationHandler.getSelectedTranslation().validateLineAsString(LudoGame.instance.getAchievementKey(true, this.translationKey))

    fun getDescription(gamePlayer: GamePlayer, displayedInLore: Boolean): MutableList<Component> {
        val description: MutableList<Component> = mutableListOf()

        for (requirement: Requirement in this.requirements) {
            val component: Component = requirement.getExplanationLine(gamePlayer, !displayedInLore)
            description.add(component)
        }

        return description
    }

    fun getIconType(gamePlayer: GamePlayer): Material {
        val hasAchievementUnlocked: Boolean = LudoGame.instance.achievementHandler.hasAchievementUnlocked(gamePlayer.uuid, this.translationKey)
        return if (hasAchievementUnlocked) Material.LIME_DYE else Material.GRAY_DYE
    }

    fun grantIfCompletedBy(gamePlayer: GamePlayer) {
        if (gamePlayer.isAI || !isCompletedBy(gamePlayer) || LudoGame.instance.achievementHandler.hasAchievementUnlocked(gamePlayer.uuid, this.translationKey)) return
        LudoGame.instance.achievementHandler.grantAchievement(gamePlayer.uuid, this.javaClass)
    }

    private fun isCompletedBy(gamePlayer: GamePlayer): Boolean = this.requirements.all { it.isCompletedBy(gamePlayer) }

}