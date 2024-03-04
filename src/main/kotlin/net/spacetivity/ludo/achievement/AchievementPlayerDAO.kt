package net.spacetivity.ludo.achievement

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object AchievementPlayerDAO : Table("achievement_players") {
    val uuid: Column<String> = varchar("uuid", 36)
    val achievementName: Column<String> = varchar("name", 30)
}