package net.spacetivity.ludo.achievement

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.achievement.container.Achievement
import java.util.*

data class AchievementPlayer(val uuid: UUID, val achievementNames: MutableList<String>) {

    fun getAdvancements(): List<Achievement> {
        return this.achievementNames.map { LudoGame.instance.achievementHandler.getAchievement(it)!! }
    }

}