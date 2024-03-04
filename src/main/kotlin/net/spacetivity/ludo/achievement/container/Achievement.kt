package net.spacetivity.ludo.achievement.container

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.player.GamePlayer
import org.bukkit.Material

open class Achievement(val name: String, val description: String, val rewardedCoins: Int, private val requirements: List<Requirement>) {

    fun getIconType(gamePlayer: GamePlayer): Material {
        val hasAchievementUnlocked: Boolean = LudoGame.instance.achievementHandler.hasAchievementUnlocked(gamePlayer.uuid, this.name)
        return if (hasAchievementUnlocked) Material.LIME_DYE else Material.LIGHT_GRAY_DYE
    }

    fun grantIfCompletedBy(gamePlayer: GamePlayer) {
        if (gamePlayer.isAI || !isCompletedBy(gamePlayer) || LudoGame.instance.achievementHandler.hasAchievementUnlocked(gamePlayer.uuid, this.name)) return
        LudoGame.instance.achievementHandler.grantAchievement(gamePlayer.uuid, this.name)
    }

    private fun isCompletedBy(gamePlayer: GamePlayer): Boolean = this.requirements.all { it.isCompletedBy(gamePlayer) }

}