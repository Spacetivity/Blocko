package net.spacetivity.blocko.entity

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.achievement.impl.MasterEliminatorAchievement
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

    // Do not remove this! (Default Type)
    VILLAGER(EntityType.VILLAGER, 0, null),

    AXOLOTL(EntityType.AXOLOTL, 10, null),
    BLAZE(EntityType.BLAZE, 10, null),
    CAT(EntityType.CAT, 10, null),
    CAVE_SPIDER(EntityType.CAVE_SPIDER, 10, null),
    SPIDER(EntityType.SPIDER, 10, null),
    CHICKEN(EntityType.CHICKEN, 10, null),
    COW(EntityType.COW, 10, null),
    CREEPER(EntityType.CREEPER, 10, null),
    DROWNED(EntityType.DROWNED, 10, null),
    ENDERMAN(EntityType.ENDERMAN, 10, null),
    EVOKER(EntityType.EVOKER, 10, null),
    FOX(EntityType.FOX, 10, null),
    FROG(EntityType.FROG, 10, null),
    HUSK(EntityType.HUSK, 10, null),
    MUSHROOM_COW(EntityType.MUSHROOM_COW, 10, null),
    OCELOT(EntityType.OCELOT, 10, null),
    PIG(EntityType.PIG, 10, null),
    PIGLIN(EntityType.PIGLIN, 10, null),
    PIGLIN_BRUTE(EntityType.PIGLIN_BRUTE, 10, null),
    PILLAGER(EntityType.PILLAGER, 10, null),
    RABBIT(EntityType.RABBIT, 10, null),
    SHEEP(EntityType.SHEEP, 10, null),
    SHULKER(EntityType.SHULKER, 10, null),
    SKELETON(EntityType.SKELETON, 10, null),
    STRAY(EntityType.STRAY, 10, null),
    VINDICATOR(EntityType.VINDICATOR, 10, null),
    WANDERING_TRADER(EntityType.WANDERING_TRADER, 10, null),
    WITCH(EntityType.WITCH, 10, null),
    WITHER_SKELETON(EntityType.WITHER_SKELETON, 10, null),
    WOLF(EntityType.WOLF, 10, null),

    ZOMBIE(EntityType.ZOMBIE, 10, null),
    ZOMBIE_VILLAGER(EntityType.ZOMBIE_VILLAGER, 10, null),
    ZOMBIFIED_PIGLIN(EntityType.ZOMBIFIED_PIGLIN, 10, null),

    BEE(EntityType.BEE, 10, null),
    PARROT(EntityType.PARROT, 10, null),
    VEX(EntityType.VEX, 10, null),

    IRON_GOLEM(EntityType.IRON_GOLEM, 10, null),
    SNOWMAN(EntityType.SNOWMAN, 10, null),

    HORSE(EntityType.HORSE, 10, null),
    ZOMBIE_HORSE(EntityType.ZOMBIE_HORSE, 10, null),
    SKELETON_HORSE(EntityType.SKELETON_HORSE, 10, null),

    MULE(EntityType.MULE, 10, null),
    DONKEY(EntityType.DONKEY, 10, null),

    LLAMA(EntityType.LLAMA, 10, null),
    TRADER_LLAMA(EntityType.TRADER_LLAMA, 10, null),

    WARDEN(EntityType.WARDEN, 100000, BlockoGame.instance.achievementHandler.getAchievement(MasterEliminatorAchievement::class.java)?.translationKey);

    fun getCorrectedTypeName(): String {
        val rawEntityTypeName: String = this.bukkitEntityType.name.lowercase()
        val entityTypeName: String

        if (rawEntityTypeName.contains("_")) {
            val words: List<String> = rawEntityTypeName.split("_")
            val correctedWordList: List<String>

            if (words.any { it.equals("llama", true) })
                correctedWordList = words.map { it.replaceFirst("l", "") }
            else
                correctedWordList = words

            entityTypeName = correctedWordList.joinToString(" ") { it.replaceFirstChar { firstChar -> firstChar.uppercase() } }
        } else {
            if (rawEntityTypeName.contains("llama", true))
                entityTypeName = rawEntityTypeName.replaceFirst("l", "").replaceFirstChar { it.uppercase() }
            else
                entityTypeName = rawEntityTypeName.replaceFirstChar { it.uppercase() }
        }

        return entityTypeName
    }

    fun getSpawnEggType(): Material {
        val typeName: String = when (this) {
            MUSHROOM_COW -> "COW_SPAWN_EGG"
            SNOWMAN -> "SNOW_GOLEM_SPAWN_EGG"
            else -> "${this.name.uppercase()}_SPAWN_EGG"
        }

        val type: Material? = Material.entries.find { it.name == typeName }
        if (type == null) println("There is no spawn egg with the name $typeName")

        return type ?: Material.BARRIER
    }

    fun isUnlockedByPlayer(uuid: UUID): Boolean {
        return BlockoGame.instance.gameEntityHandler.hasUnlockedEntityType(uuid, this)
    }

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