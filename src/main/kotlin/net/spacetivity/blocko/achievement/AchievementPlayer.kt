package net.spacetivity.blocko.achievement

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.achievement.container.Achievement
import java.util.*

data class AchievementPlayer(val uuid: UUID, val achievementNames: MutableList<String>) {

    fun getAdvancements(): List<Achievement> {
        return this.achievementNames.map { BlockoGame.instance.achievementHandler.getAchievementByKey(it)!! }
    }

    fun hasCompleted(achievement: Achievement): Boolean {
        return BlockoGame.instance.achievementHandler.hasAchievementUnlocked(this.uuid, achievement.translationKey)
    }

}