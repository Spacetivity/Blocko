package net.spacetivity.ludo.entity

import net.kyori.adventure.text.Component
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.achievement.impl.FirstEliminationAchievement
import net.spacetivity.ludo.extensions.toGamePlayerInstance
import net.spacetivity.ludo.extensions.toStatsPlayerInstance
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.stats.StatsPlayer
import net.spacetivity.ludo.stats.StatsType
import net.spacetivity.ludo.stats.UpdateOperation
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player

enum class GameEntityType(val bukkitEntityType: EntityType, val price: Int, val neededAchievementKey: String?) {

    VILLAGER(EntityType.VILLAGER, 0, null),
    WITCH(EntityType.WITCH, 1000, LudoGame.instance.achievementHandler.getAchievement(FirstEliminationAchievement::class.java)?.translationKey);

    fun getSpawnEggType(): Material? {
        return Material.entries.find { it.name == "${bukkitEntityType.name.lowercase()}_spawn_egg" }
    }

    fun buyEntityType(player: Player) {
        val gamePlayer: GamePlayer = player.toGamePlayerInstance() ?: return
        val statsPlayer: StatsPlayer = gamePlayer.toStatsPlayerInstance() ?: return

        val gameEntityHandler: GameEntityHandler = LudoGame.instance.gameEntityHandler

        if (gameEntityHandler.hasUnlockedEntityType(player.uniqueId, this)) {
            player.sendMessage(Component.text("Already unlocked!"))
            return
        }

        if (statsPlayer.coins < this.price) {
            player.sendMessage(Component.text("Not enough coins!"))
            return
        }

        gameEntityHandler.unlockEntityType(player.uniqueId, this)
        statsPlayer.update(StatsType.COINS, UpdateOperation.DECREASE, this.price)
        statsPlayer.updateDbEntry()

        player.sendMessage(Component.text("Entity type ${this.name} successfully bought!"))
        player.playSound(player.location, Sound.BLOCK_BREWING_STAND_BREW, 10F, 1F)
    }

}