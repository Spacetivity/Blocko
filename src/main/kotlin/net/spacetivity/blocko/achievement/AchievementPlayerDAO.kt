package net.spacetivity.blocko.achievement

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object AchievementPlayerDAO : Table("achievement_players") {
    val uuid: Column<String> = varchar("uuid", 36)
    val achievementId: Column<String> = varchar("achievementId", 36)
}