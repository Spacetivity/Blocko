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
import net.spacetivity.blocko.utils.NumberUtils
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import java.util.*

enum class GameEntityType(val bukkitEntityType: EntityType, val price: Int, val isBaby: Boolean, val neededAchievementKey: String?) {

    // Do not remove this! (Default Type)
    VILLAGER(EntityType.VILLAGER, 0, false, null),

    AXOLOTL(EntityType.AXOLOTL, 10, false, null),
    BLAZE(EntityType.BLAZE, 10, false, null),
    CAT(EntityType.CAT, 10, false, null),
    CAVE_SPIDER(EntityType.CAVE_SPIDER, 10, false, null),
    SPIDER(EntityType.SPIDER, 10, false, null),
    CHICKEN(EntityType.CHICKEN, 10, false, null),
    COW(EntityType.COW, 10, false, null),
    CREEPER(EntityType.CREEPER, 10, false, null),
    DROWNED(EntityType.DROWNED, 10, false, null),
    ENDERMAN(EntityType.ENDERMAN, 10, false, null),
    EVOKER(EntityType.EVOKER, 10, false, null),
    FOX(EntityType.FOX, 10, false, null),
    FROG(EntityType.FROG, 10, false, null),
    HUSK(EntityType.HUSK, 10, false, null),
    MUSHROOM_COW(EntityType.MUSHROOM_COW, 10, false, null),
    OCELOT(EntityType.OCELOT, 10, false, null),
    PIG(EntityType.PIG, 10, false, null),
    PIGLIN(EntityType.PIGLIN, 10, false, null),
    PIGLIN_BRUTE(EntityType.PIGLIN_BRUTE, 10, false, null),
    PILLAGER(EntityType.PILLAGER, 10, false, null),
    RABBIT(EntityType.RABBIT, 10, false, null),
    SHEEP(EntityType.SHEEP, 10, false, null),
    SHULKER(EntityType.SHULKER, 10, false, null),
    SKELETON(EntityType.SKELETON, 10, false, null),
    STRAY(EntityType.STRAY, 10, false, null),
    VINDICATOR(EntityType.VINDICATOR, 10, false, null),
    WANDERING_TRADER(EntityType.WANDERING_TRADER, 10, false, null),
    WITCH(EntityType.WITCH, 10, false, null),
    WITHER_SKELETON(EntityType.WITHER_SKELETON, 10, false, null),
    WOLF(EntityType.WOLF, 10, false, null),

    ZOMBIE(EntityType.ZOMBIE, 10, false, null),
    ZOMBIE_VILLAGER(EntityType.ZOMBIE_VILLAGER, 10, false, null),
    ZOMBIFIED_PIGLIN(EntityType.ZOMBIFIED_PIGLIN, 10, false, null),

    BEE(EntityType.BEE, 10, false, null),
    PARROT(EntityType.PARROT, 10, false, null),
    VEX(EntityType.VEX, 10, false, null),

    IRON_GOLEM(EntityType.IRON_GOLEM, 10, false, null),

    HORSE(EntityType.HORSE, 10, true, null),
    ZOMBIE_HORSE(EntityType.ZOMBIE_HORSE, 10, true, null),
    SKELETON_HORSE(EntityType.SKELETON_HORSE, 10, true, null),

    MULE(EntityType.MULE, 10, false, null),
    DONKEY(EntityType.DONKEY, 10, false, null),

    LLAMA(EntityType.LLAMA, 10, false, null),
    TRADER_LLAMA(EntityType.TRADER_LLAMA, 10, false, null),

    POLAR_BEAR(EntityType.POLAR_BEAR, 10, true, null),
    PANDA(EntityType.PANDA, 10, true, null),
    CAMEL(EntityType.CAMEL, 10, true, null),
    SNIFFER(EntityType.SNIFFER, 10, true, null),

    COD(EntityType.COD, 10, false, null),
    SALMON(EntityType.SALMON, 10, false, null),
    TROPICAL_FISH(EntityType.TROPICAL_FISH, 10, false, null),
    DOLPHIN(EntityType.DOLPHIN, 10, false, null),

    WARDEN(EntityType.WARDEN, 100000, false, BlockoGame.instance.achievementHandler.getAchievement(MasterEliminatorAchievement::class.java)?.translationKey);

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
            Placeholder.parsed("entity_type_name", getCorrectedTypeName()),
            Placeholder.parsed("amount", NumberUtils.format(this.price)))
    }

}