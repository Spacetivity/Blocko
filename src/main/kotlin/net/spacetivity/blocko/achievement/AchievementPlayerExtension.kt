package net.spacetivity.blocko.achievement

import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.achievement.container.Achievement
import net.spacetivity.blocko.player.GamePlayer
import kotlin.reflect.KClass

fun <T : Achievement> getAchievementByClass(clazz: KClass<T>): Achievement? {
    return Blocko.instance.achievementHandler.getAchievement(clazz.java)
}

fun <T : Achievement> GamePlayer.grantIfCompletedBy(clazz: KClass<T>) {
    Blocko.instance.achievementHandler.getAchievement(clazz.java)?.grantIfCompletedBy(this)
}