package net.spacetivity.blocko.achievement

import org.jetbrains.exposed.sql.Table

object AchievementPlayerDAO : Table("achievement_players") {
    val uuid = varchar("uuid", 36)
    val achievementId = varchar("achievementId", 36)
}