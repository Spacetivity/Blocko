package net.spacetivity.ludo.advancement.container

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.player.GamePlayer
import org.bukkit.Material

open class Achievement(val name: String, val description: String, val icon: Material, private val requirements: List<Requirement>) {

    fun grantIfCompletedBy(gamePlayer: GamePlayer) {
        if (!isCompletedBy(gamePlayer) || LudoGame.instance.achievementHandler.hasAchievementUnlocked(gamePlayer.uuid, this.name)) return
        LudoGame.instance.achievementHandler.grantAchievement(gamePlayer.uuid, this.name)
    }

    private fun isCompletedBy(gamePlayer: GamePlayer): Boolean = this.requirements.all { it.isCompletedBy(gamePlayer) }

}