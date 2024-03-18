package net.spacetivity.blocko.achievement

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.achievement.container.Achievement
import net.spacetivity.blocko.extensions.addCoins
import net.spacetivity.blocko.extensions.toGamePlayerInstance
import net.spacetivity.blocko.extensions.translateMessage
import net.spacetivity.blocko.player.GamePlayer
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

    val cachedAchievements: MutableList<Achievement> = mutableListOf()
    private val cachedAchievementPlayers: MutableList<AchievementPlayer> = mutableListOf()

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

            for (resultRow: ResultRow in AchievementPlayerDAO.select() { AchievementPlayerDAO.uuid eq uuid.toString() }.toMutableList()) {
                val achievementName: String = resultRow[AchievementPlayerDAO.achievementId]
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
        val achievementPlayer: AchievementPlayer = getAchievementPlayer(uuid) ?: return false
        return achievementPlayer.achievementNames.contains(translationKey)
    }

    fun <T : Achievement> grantAchievement(uuid: UUID, achievementClass: Class<T>) {
        val achievement: Achievement = getAchievement(achievementClass) ?: return

        transaction {
            AchievementPlayerDAO.insert { statement: InsertStatement<Number> ->
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

        val player: Player = Bukkit.getPlayer(uuid) ?: return
        val gamePlayer: GamePlayer = player.toGamePlayerInstance() ?: return

        val hoverText: Component = achievement.getDescription(gamePlayer)[0]

        player.translateMessage("blocko.achievement.unlocked", Placeholder.parsed("name", achievement.name), Placeholder.component("hover_text", hoverText))
        player.playSound(player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5F, 1.0F)
        if (achievement.rewardedCoins > 0) player.addCoins(achievement.rewardedCoins, false)
    }

}