package net.spacetivity.blocko.achievement.container

import net.kyori.adventure.text.Component
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.player.GamePlayer

open class Achievement(val translationKey: String, val rewardedCoins: Int, private val requirements: List<Requirement>) {

    val name: String = BlockoGame.instance.translationHandler.getSelectedTranslation().validateLineAsString(BlockoGame.instance.getAchievementKey(true, this.translationKey))

    fun getDescription(gamePlayer: GamePlayer): MutableList<Component> {
        val description: MutableList<Component> = mutableListOf()

        for (requirement: Requirement in this.requirements) {
            val component: Component = requirement.getExplanationLine(gamePlayer)
            description.add(component)
        }

        return description
    }

    fun grantIfCompletedBy(gamePlayer: GamePlayer) {
        if (gamePlayer.isAI || !isCompletedBy(gamePlayer) || BlockoGame.instance.achievementHandler.hasAchievementUnlocked(gamePlayer.uuid, this.translationKey)) return
        BlockoGame.instance.achievementHandler.grantAchievement(gamePlayer.uuid, this.javaClass)
    }

    private fun isCompletedBy(gamePlayer: GamePlayer): Boolean = this.requirements.all { it.isCompletedBy(gamePlayer) }

}