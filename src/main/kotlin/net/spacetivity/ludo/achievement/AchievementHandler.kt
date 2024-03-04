package net.spacetivity.ludo.achievement

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.ludo.achievement.container.Achievement
import net.spacetivity.ludo.extensions.addCoins
import net.spacetivity.ludo.extensions.translateMessage
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class AchievementHandler {

    private val cachedAchievements: MutableList<Achievement> = mutableListOf()
    private val cachedAchievementPlayers: MutableList<AchievementPlayer> = mutableListOf()

    fun registerAchievement(achievement: Achievement) {
        this.cachedAchievements.add(achievement)
    }

    fun getAchievement(name: String): Achievement? {
        return this.cachedAchievements.find { it.name == name }
    }

    fun createOrLoadAchievementPlayer(uuid: UUID) {
        transaction {
            val resultRow: ResultRow? = AchievementPlayerDAO.select { AchievementPlayerDAO.uuid eq uuid.toString() }.limit(1).firstOrNull()
            val achievementPlayer = AchievementPlayer(uuid, mutableListOf())
            cachedAchievementPlayers.add(achievementPlayer)

            if (resultRow != null) {
                val achievementName: String = resultRow[AchievementPlayerDAO.achievementName]
                if (cachedAchievements.none { it.name == achievementName }) return@transaction
                achievementPlayer.achievementNames.add(achievementName)
            }
        }
    }

    fun unloadAchievementPlayer(uuid: UUID) {
        this.cachedAchievementPlayers.removeIf { it.uuid == uuid }
    }

    fun getAchievementPlayer(uuid: UUID): AchievementPlayer? {
        return this.cachedAchievementPlayers.find { it.uuid == uuid }
    }

    fun hasAchievementUnlocked(uuid: UUID, name: String): Boolean {
        if (this.cachedAchievements.none { it.name == name }) return false
        val achievementPlayer: AchievementPlayer = getAchievementPlayer(uuid) ?: return false
        return achievementPlayer.achievementNames.contains(name)
    }

    fun grantAchievement(uuid: UUID, achievementName: String) {
        val achievement: Achievement = getAchievement(achievementName) ?: return

        transaction {
            AchievementPlayerDAO.insert { statement: InsertStatement<Number> ->
                statement[AchievementPlayerDAO.uuid] = uuid.toString()
                statement[AchievementPlayerDAO.achievementName] = achievementName
            }
        }

        var achievementPlayer: AchievementPlayer? = getAchievementPlayer(uuid)

        if (achievementPlayer == null) {
           achievementPlayer = AchievementPlayer(uuid, mutableListOf(achievementName))
            this.cachedAchievementPlayers.add(achievementPlayer)
        } else {
            achievementPlayer.achievementNames.add(achievementName)
        }

        val player: Player = Bukkit.getPlayer(uuid) ?: return
        player.translateMessage("blocko.achievement.unlocked", Placeholder.parsed("name", achievementName))
        player.playSound(player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5F, 1.0F)
        player.addCoins(achievement.rewardedCoins)
    }

}