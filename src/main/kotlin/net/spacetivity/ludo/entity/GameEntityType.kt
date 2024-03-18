package net.spacetivity.ludo.entity

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.achievement.impl.FirstEliminationAchievement
import net.spacetivity.ludo.extensions.toGamePlayerInstance
import net.spacetivity.ludo.extensions.toStatsPlayerInstance
import net.spacetivity.ludo.extensions.translateMessage
import net.spacetivity.ludo.stats.StatsPlayer
import net.spacetivity.ludo.stats.StatsType
import net.spacetivity.ludo.stats.UpdateOperation
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import java.util.*

enum class GameEntityType(val bukkitEntityType: EntityType, val price: Int, val neededAchievementKey: String?) {

    VILLAGER(EntityType.VILLAGER, 0, null),
    WITCH(EntityType.WITCH, 1000, LudoGame.instance.achievementHandler.getAchievement(FirstEliminationAchievement::class.java)?.translationKey);

    fun getSpawnEggType(): Material? {
        return Material.entries.find { it.name == "${bukkitEntityType.name.uppercase()}_SPAWN_EGG" }
    }

    fun isUnlockedByPlayer(uuid: UUID): Boolean = LudoGame.instance.gameEntityHandler.hasUnlockedEntityType(uuid, this)

    fun buyEntityType(player: Player) {
        if (isUnlockedByPlayer(player.uniqueId)) return

        val statsPlayer: StatsPlayer = player.toGamePlayerInstance()?.toStatsPlayerInstance() ?: return

        LudoGame.instance.gameEntityHandler.unlockEntityType(player.uniqueId, this)
        statsPlayer.update(StatsType.COINS, UpdateOperation.DECREASE, this.price)
        statsPlayer.updateDbEntry()

        player.playSound(player.location, Sound.BLOCK_BREWING_STAND_BREW, 10F, 1F)
        player.translateMessage("blocko.entity_shop.successfully_bought_entity_type",
            Placeholder.parsed("entity_type_name", this.bukkitEntityType.name),
            Placeholder.parsed("amount", this.price.toString()))
    }

}