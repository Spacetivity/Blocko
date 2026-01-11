package net.spacetivity.blocko.entity

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.achievement.grantIfCompletedBy
import net.spacetivity.blocko.achievement.impl.EntityCollectorAchievement
import net.spacetivity.blocko.arena.toGamePlayerInstance
import net.spacetivity.blocko.stats.StatsType
import net.spacetivity.blocko.stats.toStatsPlayerInstance
import net.spacetivity.blocko.translation.translateMessage
import net.spacetivity.blocko.utils.NumberUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import java.util.*

enum class GameEntityType(val bukkitEntityType: EntityType, val defaultProperties: GameEntityProperties) {

    // Do not remove this! (Default Type)
    VILLAGER(EntityType.VILLAGER, GameEntityProperties(0, false)),

    // Basic Passive Mobs (30-50)
    CHICKEN(EntityType.CHICKEN, GameEntityProperties(30, false)),
    PIG(EntityType.PIG, GameEntityProperties(30, false)),
    SHEEP(EntityType.SHEEP, GameEntityProperties(30, false)),
    COW(EntityType.COW, GameEntityProperties(35, false)),
    GOAT(EntityType.GOAT, GameEntityProperties(35, false)),
    RABBIT(EntityType.RABBIT, GameEntityProperties(40, false, "first_elimination")),
    FROG(EntityType.FROG, GameEntityProperties(40, false)),
    TURTLE(EntityType.TURTLE, GameEntityProperties(40, false)),
    TADPOLE(EntityType.TADPOLE, GameEntityProperties(25, false)),
    CAT(EntityType.CAT, GameEntityProperties(45, false)),
    OCELOT(EntityType.OCELOT, GameEntityProperties(45, false)),
    FOX(EntityType.FOX, GameEntityProperties(45, false)),
    ARMADILLO(EntityType.ARMADILLO, GameEntityProperties(45, false)),
    WOLF(EntityType.WOLF, GameEntityProperties(50, false)),
    AXOLOTL(EntityType.AXOLOTL, GameEntityProperties(50, false)),

    // Common Hostile Mobs (50-80)
    ZOMBIE(EntityType.ZOMBIE, GameEntityProperties(50, false)),
    SKELETON(EntityType.SKELETON, GameEntityProperties(55, false)),
    SPIDER(EntityType.SPIDER, GameEntityProperties(55, false)),
    CAVE_SPIDER(EntityType.CAVE_SPIDER, GameEntityProperties(60, false)),
    CREEPER(EntityType.CREEPER, GameEntityProperties(65, false)),
    DROWNED(EntityType.DROWNED, GameEntityProperties(60, false, "first_game")),
    HUSK(EntityType.HUSK, GameEntityProperties(60, false)),
    BOGGED(EntityType.BOGGED, GameEntityProperties(60, false)),
    ZOMBIE_VILLAGER(EntityType.ZOMBIE_VILLAGER, GameEntityProperties(65, false)),
    ZOMBIFIED_PIGLIN(EntityType.ZOMBIFIED_PIGLIN, GameEntityProperties(65, false)),
    PIGLIN(EntityType.PIGLIN, GameEntityProperties(70, false)),
    ENDERMITE(EntityType.ENDERMITE, GameEntityProperties(70, false)),
    ENDERMAN(EntityType.ENDERMAN, GameEntityProperties(75, false)),
    BLAZE(EntityType.BLAZE, GameEntityProperties(80, false)),
    VEX(EntityType.VEX, GameEntityProperties(80, false)),

    // Uncommon/Moderate Hostile Mobs (80-120)
    STRAY(EntityType.STRAY, GameEntityProperties(85, false)),
    PARCHED(EntityType.PARCHED, GameEntityProperties(85, false)),
    PILLAGER(EntityType.PILLAGER, GameEntityProperties(90, false)),
    ILLUSIONER(EntityType.ILLUSIONER, GameEntityProperties(90, false)),
    WITCH(EntityType.WITCH, GameEntityProperties(100, false)),
    EVOKER(EntityType.EVOKER, GameEntityProperties(110, false)),
    PIGLIN_BRUTE(EntityType.PIGLIN_BRUTE, GameEntityProperties(110, false)),
    VINDICATOR(EntityType.VINDICATOR, GameEntityProperties(120, false)),
    WITHER_SKELETON(EntityType.WITHER_SKELETON, GameEntityProperties(120, false)),
    SHULKER(EntityType.SHULKER, GameEntityProperties(120, false)),

    // Rare/Special Mobs (120-200)
    HOGLIN(EntityType.HOGLIN, GameEntityProperties(130, true)),
    ZOGLIN(EntityType.ZOGLIN, GameEntityProperties(130, true)),
    STRIDER(EntityType.STRIDER, GameEntityProperties(140, true)),
    BREEZE(EntityType.BREEZE, GameEntityProperties(150, false)),
    GLOW_SQUID(EntityType.GLOW_SQUID, GameEntityProperties(150, false)),
    DOLPHIN(EntityType.DOLPHIN, GameEntityProperties(160, false)),
    NAUTILUS(EntityType.NAUTILUS, GameEntityProperties(180, false)),
    ZOMBIE_NAUTILUS(EntityType.ZOMBIE_NAUTILUS, GameEntityProperties(180, false)),

    // Aquatic Mobs (40-60)
    COD(EntityType.COD, GameEntityProperties(40, false)),
    SALMON(EntityType.SALMON, GameEntityProperties(40, false)),
    TROPICAL_FISH(EntityType.TROPICAL_FISH, GameEntityProperties(50, false)),

    // Flying/Passive Special (50-70)
    BEE(EntityType.BEE, GameEntityProperties(50, false)),
    PARROT(EntityType.PARROT, GameEntityProperties(55, false)),
    ALLAY(EntityType.ALLAY, GameEntityProperties(60, false)),

    // Mounts (70-100)
    MULE(EntityType.MULE, GameEntityProperties(70, false)),
    DONKEY(EntityType.DONKEY, GameEntityProperties(70, false)),
    LLAMA(EntityType.LLAMA, GameEntityProperties(80, false)),
    TRADER_LLAMA(EntityType.TRADER_LLAMA, GameEntityProperties(90, false)),
    HORSE(EntityType.HORSE, GameEntityProperties(100, true)),
    ZOMBIE_HORSE(EntityType.ZOMBIE_HORSE, GameEntityProperties(100, true)),
    SKELETON_HORSE(EntityType.SKELETON_HORSE, GameEntityProperties(100, true)),

    // Large Passive Mobs (100-120)
    POLAR_BEAR(EntityType.POLAR_BEAR, GameEntityProperties(100, true)),
    PANDA(EntityType.PANDA, GameEntityProperties(100, true)),
    CAMEL(EntityType.CAMEL, GameEntityProperties(110, true)),
    CAMEL_HUSK(EntityType.CAMEL_HUSK, GameEntityProperties(110, true)),
    SNIFFER(EntityType.SNIFFER, GameEntityProperties(120, true)),

    // Achievement Mobs (500-1000)
    IRON_GOLEM(EntityType.IRON_GOLEM, GameEntityProperties(500, false, "win_monster")),
    MOOSHROOM(EntityType.MOOSHROOM, GameEntityProperties(800, false, "entity_collector")),
    WANDERING_TRADER(EntityType.WANDERING_TRADER, GameEntityProperties(1000, false, "entity_collector")),

    // Boss Mobs (2000+)
    WARDEN(EntityType.WARDEN, GameEntityProperties(2500, false, "master_eliminator"));

    fun getProperties(): GameEntityProperties {
        val entityProperties = Blocko.instance.gameEntityPropertiesFiles[this.name.lowercase()]?.gameEntityProperties
        return entityProperties ?: this.defaultProperties
    }

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
        if (type == null) Bukkit.getConsoleSender()
            .sendMessage(Component.text("There is no spawn egg with the name $typeName", NamedTextColor.DARK_RED))
        return type ?: Material.BARRIER
    }

    fun isUnlockedByPlayer(uuid: UUID): Boolean {
        return Blocko.instance.gameEntityHandler.hasUnlockedEntityType(uuid, this)
    }

    fun buyEntityType(player: Player) {
        if (isUnlockedByPlayer(player.uniqueId)) return

        val gamePlayer = player.toGamePlayerInstance() ?: return
        val statsPlayer = gamePlayer.toStatsPlayerInstance() ?: return

        val properties = getProperties()

        Blocko.instance.gameEntityHandler.unlockEntityType(player.uniqueId, this)
        statsPlayer.update(StatsType.COINS, false, properties.price)
        statsPlayer.updateDbEntry()

        gamePlayer.grantIfCompletedBy(EntityCollectorAchievement::class)

        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 10F, 1F)
        player.translateMessage(
            "blocko.entity_shop.successfully_bought_entity_type",
            Placeholder.parsed("entity_type_name", getCorrectedTypeName()),
            Placeholder.parsed("amount", NumberUtils.format(properties.price))
        )
    }

}