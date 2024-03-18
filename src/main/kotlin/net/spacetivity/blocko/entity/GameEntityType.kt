package net.spacetivity.blocko.entity

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.achievement.impl.FirstEliminationAchievement
import net.spacetivity.blocko.extensions.toGamePlayerInstance
import net.spacetivity.blocko.extensions.toStatsPlayerInstance
import net.spacetivity.blocko.extensions.translateMessage
import net.spacetivity.blocko.stats.StatsPlayer
import net.spacetivity.blocko.stats.StatsType
import net.spacetivity.blocko.stats.UpdateOperation
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import java.util.*

enum class GameEntityType(val bukkitEntityType: EntityType, val price: Int, val neededAchievementKey: String?) {

    VILLAGER(EntityType.VILLAGER, 0, null),
    SKELETON(EntityType.SKELETON, 10, null),
    WITCH(EntityType.WITCH, 1000, BlockoGame.instance.achievementHandler.getAchievement(FirstEliminationAchievement::class.java)?.translationKey);

    fun getSpawnEggType(): Material? {
        return Material.entries.find { it.name == "${bukkitEntityType.name.uppercase()}_SPAWN_EGG" }
    }

    fun isUnlockedByPlayer(uuid: UUID): Boolean = BlockoGame.instance.gameEntityHandler.hasUnlockedEntityType(uuid, this)

    fun buyEntityType(player: Player) {
        if (isUnlockedByPlayer(player.uniqueId)) return

        val statsPlayer: StatsPlayer = player.toGamePlayerInstance()?.toStatsPlayerInstance() ?: return

        BlockoGame.instance.gameEntityHandler.unlockEntityType(player.uniqueId, this)
        statsPlayer.update(StatsType.COINS, UpdateOperation.DECREASE, this.price)
        statsPlayer.updateDbEntry()

        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 10F, 1F)
        player.translateMessage("blocko.entity_shop.successfully_bought_entity_type",
            Placeholder.parsed("entity_type_name", this.bukkitEntityType.name),
            Placeholder.parsed("amount", this.price.toString()))
    }

}