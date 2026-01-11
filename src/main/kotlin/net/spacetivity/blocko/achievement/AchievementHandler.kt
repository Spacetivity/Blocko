package net.spacetivity.blocko.achievement

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.achievement.container.Achievement
import net.spacetivity.blocko.arena.toGamePlayerInstance
import net.spacetivity.blocko.stats.addCoins
import net.spacetivity.blocko.translation.translateMessage
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class AchievementHandler {

    val cachedAchievements = mutableListOf<Achievement>()
    private val cachedAchievementPlayers = mutableListOf<AchievementPlayer>()

    fun registerAchievement(achievement: Achievement) {
        this.cachedAchievements.add(achievement)
    }

    fun <T : Achievement> getAchievement(clazz: Class<T>): Achievement? {
        return this.cachedAchievements.find { it.javaClass.name == clazz.name }
    }

    fun getAchievementByKey(translationKey: String): Achievement? {
        return this.cachedAchievements.find { it.translationKey == translationKey }
    }

    fun createOrLoadAchievementPlayer(uuid: UUID) {
        transaction {
            val achievementPlayer = AchievementPlayer(uuid, mutableListOf())
            cachedAchievementPlayers.add(achievementPlayer)

            for (resultRow in AchievementPlayerDAO.selectAll().where { AchievementPlayerDAO.uuid eq uuid.toString() }.toMutableList()) {
                val achievementName = resultRow[AchievementPlayerDAO.achievementId]
                if (cachedAchievements.none { it.translationKey == achievementName }) return@transaction
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

    fun hasAchievementUnlocked(uuid: UUID, translationKey: String): Boolean {
        if (this.cachedAchievements.none { it.translationKey == translationKey }) return false
        val achievementPlayer = getAchievementPlayer(uuid) ?: return false
        return achievementPlayer.achievementNames.contains(translationKey)
    }

    fun <T : Achievement> grantAchievement(uuid: UUID, achievementClass: Class<T>) {
        val achievement = getAchievement(achievementClass) ?: return

        transaction {
            AchievementPlayerDAO.insert { statement ->
                statement[AchievementPlayerDAO.uuid] = uuid.toString()
                statement[achievementId] = achievement.translationKey
            }
        }

        var achievementPlayer: AchievementPlayer? = getAchievementPlayer(uuid)

        if (achievementPlayer == null) {
            achievementPlayer = AchievementPlayer(uuid, mutableListOf(achievement.translationKey))
            this.cachedAchievementPlayers.add(achievementPlayer)
        } else {
            achievementPlayer.achievementNames.add(achievement.translationKey)
        }

        val player = Bukkit.getPlayer(uuid) ?: return
        val gamePlayer = player.toGamePlayerInstance() ?: return

        player.translateMessage("blocko.achievement.unlocked", Placeholder.parsed("name", achievement.name), Placeholder.component("hover_text", achievement.getDescription(gamePlayer)[0]))
        player.playSound(player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5F, 1.0F)
        if (achievement.rewardedCoins > 0) player.addCoins(achievement.rewardedCoins, false)
    }

}