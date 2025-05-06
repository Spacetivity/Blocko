package net.spacetivity.blocko.achievement

import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.achievement.container.Achievement
import java.util.*

data class AchievementPlayer(val uuid: UUID, val achievementNames: MutableList<String>) {

    fun getAdvancements(): List<Achievement> {
        return this.achievementNames.map { Blocko.instance.achievementHandler.getAchievementByKey(it)!! }
    }

    fun hasCompleted(achievement: Achievement): Boolean {
        return Blocko.instance.achievementHandler.hasAchievementUnlocked(this.uuid, achievement.translationKey)
    }

}