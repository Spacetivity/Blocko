package net.spacetivity.blocko.achievement

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.achievement.container.Achievement
import net.spacetivity.blocko.player.GamePlayer
import kotlin.reflect.KClass

fun <T : Achievement> getAchievementByClass(clazz: KClass<T>): Achievement? {
    return BlockoGame.instance.achievementHandler.getAchievement(clazz.java)
}

fun <T : Achievement> GamePlayer.grantIfCompletedBy(clazz: KClass<T>) {
    BlockoGame.instance.achievementHandler.getAchievement(clazz.java)?.grantIfCompletedBy(this)
}