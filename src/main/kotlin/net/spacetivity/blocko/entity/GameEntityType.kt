package net.spacetivity.blocko.entity

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.achievement.container.Achievement
import net.spacetivity.blocko.achievement.grantIfCompletedBy
import net.spacetivity.blocko.achievement.impl.*
import net.spacetivity.blocko.arena.toGamePlayerInstance
import net.spacetivity.blocko.stats.StatsType
import net.spacetivity.blocko.stats.UpdateOperation
import net.spacetivity.blocko.stats.toStatsPlayerInstance
import net.spacetivity.blocko.translation.translateMessage
import net.spacetivity.blocko.utils.NumberUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import java.util.*
import kotlin.reflect.KClass

enum class GameEntityType(val bukkitEntityType: EntityType, val price: Int, val isBaby: Boolean, val achievementClass: KClass<out Achievement>?) {

    // Do not remove this! (Default Type)
    VILLAGER(EntityType.VILLAGER, 0, false, null),

    AXOLOTL(EntityType.AXOLOTL, 50, false, null),
    BLAZE(EntityType.BLAZE, 75, false, null),
    CAT(EntityType.CAT, 60, false, null),
    CAVE_SPIDER(EntityType.CAVE_SPIDER, 70, false, null),
    SPIDER(EntityType.SPIDER, 70, false, null),
    CHICKEN(EntityType.CHICKEN, 40, false, null),
    COW(EntityType.COW, 50, false, null),
    GOAT(EntityType.GOAT, 10, false, null),
    CREEPER(EntityType.CREEPER, 80, false, null),
    DROWNED(EntityType.DROWNED, 70, false, PlayFirstGameAchievement::class),
    ENDERMAN(EntityType.ENDERMAN, 80, false, null),
    EVOKER(EntityType.EVOKER, 120, false, null),
    FOX(EntityType.FOX, 60, false, null),
    ARMADILLO(EntityType.ARMADILLO, 60, false, null),
    FROG(EntityType.FROG, 40, false, null),
    TURTLE(EntityType.TURTLE, 45, false, null),
    HUSK(EntityType.HUSK, 70, false, null),
    MOOSHROOM(EntityType.MOOSHROOM, 3500, false, EntityCollectorAchievement::class),
    OCELOT(EntityType.OCELOT, 50, false, null),
    PIG(EntityType.PIG, 40, false, null),
    PIGLIN(EntityType.PIGLIN, 70, false, null),
    PIGLIN_BRUTE(EntityType.PIGLIN_BRUTE, 120, false, null),
    ZOGLIN(EntityType.ZOGLIN, 120, true, null),
    HOGLIN(EntityType.HOGLIN, 120, true, null),
    PILLAGER(EntityType.PILLAGER, 90, false, null),
    ILLUSIONER(EntityType.ILLUSIONER, 90, false, null),
    RABBIT(EntityType.RABBIT, 40, false, FirstEliminationAchievement::class),
    SHEEP(EntityType.SHEEP, 40, false, null),
    SHULKER(EntityType.SHULKER, 150, false, null),
    SKELETON(EntityType.SKELETON, 90, false, null),
    STRAY(EntityType.STRAY, 90, false, null),
    VINDICATOR(EntityType.VINDICATOR, 150, false, null),
    WANDERING_TRADER(EntityType.WANDERING_TRADER, 1050, false, EntityCollectorAchievement::class),
    WITCH(EntityType.WITCH, 150, false, null),
    WITHER_SKELETON(EntityType.WITHER_SKELETON, 150, false, null),
    WOLF(EntityType.WOLF, 50, false, null),

    BOGGED(EntityType.BOGGED, 70, false, null),
    ZOMBIE(EntityType.ZOMBIE, 70, false, null),
    ZOMBIE_VILLAGER(EntityType.ZOMBIE_VILLAGER, 70, false, null),
    ZOMBIFIED_PIGLIN(EntityType.ZOMBIFIED_PIGLIN, 70, false, null),

    BEE(EntityType.BEE, 50, false, null),
    PARROT(EntityType.PARROT, 50, false, null),
    VEX(EntityType.VEX, 75, false, null),

    IRON_GOLEM(EntityType.IRON_GOLEM, 350, false, WinMonsterAchievement::class),

    HORSE(EntityType.HORSE, 100, true, null),
    ZOMBIE_HORSE(EntityType.ZOMBIE_HORSE, 100, true, null),
    SKELETON_HORSE(EntityType.SKELETON_HORSE, 100, true, null),

    MULE(EntityType.MULE, 70, false, null),
    DONKEY(EntityType.DONKEY, 70, false, null),

    LLAMA(EntityType.LLAMA, 70, false, null),
    TRADER_LLAMA(EntityType.TRADER_LLAMA, 100, false, null),

    POLAR_BEAR(EntityType.POLAR_BEAR, 100, true, null),
    PANDA(EntityType.PANDA, 100, true, null),
    CAMEL(EntityType.CAMEL, 100, true, null),
    SNIFFER(EntityType.SNIFFER, 100, true, null),

    COD(EntityType.COD, 40, false, null),
    SALMON(EntityType.SALMON, 40, false, null),
    TROPICAL_FISH(EntityType.TROPICAL_FISH, 40, false, null),
    DOLPHIN(EntityType.DOLPHIN, 100, false, null),

    WARDEN(EntityType.WARDEN, 15000, false, MasterEliminatorAchievement::class);

    fun getCorrectedTypeName(): String {
        val rawEntityTypeName = this.bukkitEntityType.name.lowercase()
        return if (rawEntityTypeName.contains("_")) {
            val words = rawEntityTypeName.split("_")
            words.joinToString(" ") { it.replaceFirstChar { firstChar -> firstChar.uppercase() } }
        } else {
            rawEntityTypeName.replaceFirstChar { it.uppercase() }
        }
    }

    fun getSpawnEggType(): Material {
        val typeName = "${this.name.uppercase()}_SPAWN_EGG"
        val type = Material.entries.find { it.name == typeName }
        if (type == null) Bukkit.getConsoleSender().sendMessage(Component.text("There is no spawn egg with the name $typeName", NamedTextColor.DARK_RED))
        return type ?: Material.BARRIER
    }

    fun isUnlockedByPlayer(uuid: UUID): Boolean {
        return BlockoGame.instance.gameEntityHandler.hasUnlockedEntityType(uuid, this)
    }

    fun buyEntityType(player: Player) {
        if (isUnlockedByPlayer(player.uniqueId)) return

        val gamePlayer = player.toGamePlayerInstance() ?: return
        val statsPlayer = gamePlayer.toStatsPlayerInstance() ?: return

        BlockoGame.instance.gameEntityHandler.unlockEntityType(player.uniqueId, this)
        statsPlayer.update(StatsType.COINS, UpdateOperation.DECREASE, this.price)
        statsPlayer.updateDbEntry()

        gamePlayer.grantIfCompletedBy(EntityCollectorAchievement::class)

        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 10F, 1F)
        player.translateMessage("blocko.entity_shop.successfully_bought_entity_type",
            Placeholder.parsed("entity_type_name", getCorrectedTypeName()),
            Placeholder.parsed("amount", NumberUtils.format(this.price)))
    }

}