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

    AXOLOTL(EntityType.AXOLOTL, GameEntityProperties(50, false)),
    BLAZE(EntityType.BLAZE, GameEntityProperties(75, false)),
    CAT(EntityType.CAT, GameEntityProperties(60, false)),
    CAVE_SPIDER(EntityType.CAVE_SPIDER, GameEntityProperties(70, false)),
    SPIDER(EntityType.SPIDER, GameEntityProperties(70, false)),
    CHICKEN(EntityType.CHICKEN, GameEntityProperties(40, false)),
    COW(EntityType.COW, GameEntityProperties(50, false)),
    GOAT(EntityType.GOAT, GameEntityProperties(10, false)),
    CREEPER(EntityType.CREEPER, GameEntityProperties(80, false)),
    DROWNED(EntityType.DROWNED, GameEntityProperties(70, false, "first_game")),
    ENDERMAN(EntityType.ENDERMAN, GameEntityProperties(80, false)),
    EVOKER(EntityType.EVOKER, GameEntityProperties(120, false)),
    FOX(EntityType.FOX, GameEntityProperties(60, false)),
    ARMADILLO(EntityType.ARMADILLO, GameEntityProperties(60, false)),
    FROG(EntityType.FROG, GameEntityProperties(40, false)),
    TURTLE(EntityType.TURTLE, GameEntityProperties(45, false)),
    HUSK(EntityType.HUSK, GameEntityProperties(70, false)),
    MOOSHROOM(EntityType.MOOSHROOM, GameEntityProperties(3500, false, "entity_collector")),
    OCELOT(EntityType.OCELOT, GameEntityProperties(50, false)),
    PIG(EntityType.PIG, GameEntityProperties(40, false)),
    PIGLIN(EntityType.PIGLIN, GameEntityProperties(70, false)),
    PIGLIN_BRUTE(EntityType.PIGLIN_BRUTE, GameEntityProperties(120, false)),
    ZOGLIN(EntityType.ZOGLIN, GameEntityProperties(120, true)),
    HOGLIN(EntityType.HOGLIN, GameEntityProperties(120, true)),
    PILLAGER(EntityType.PILLAGER, GameEntityProperties(90, false)),
    ILLUSIONER(EntityType.ILLUSIONER, GameEntityProperties(90, false)),
    RABBIT(EntityType.RABBIT, GameEntityProperties(40, false, "first_elimination")),
    SHEEP(EntityType.SHEEP, GameEntityProperties(40, false)),
    SHULKER(EntityType.SHULKER, GameEntityProperties(150, false)),
    SKELETON(EntityType.SKELETON, GameEntityProperties(90, false)),
    STRAY(EntityType.STRAY, GameEntityProperties(90, false)),
    VINDICATOR(EntityType.VINDICATOR, GameEntityProperties(150, false)),
    WANDERING_TRADER(EntityType.WANDERING_TRADER, GameEntityProperties(1050, false, "entity_collector")),
    WITCH(EntityType.WITCH, GameEntityProperties(150, false)),
    WITHER_SKELETON(EntityType.WITHER_SKELETON, GameEntityProperties(150, false)),
    WOLF(EntityType.WOLF, GameEntityProperties(50, false)),

    BOGGED(EntityType.BOGGED, GameEntityProperties(70, false)),
    ZOMBIE(EntityType.ZOMBIE, GameEntityProperties(70, false)),
    ZOMBIE_VILLAGER(EntityType.ZOMBIE_VILLAGER, GameEntityProperties(70, false)),
    ZOMBIFIED_PIGLIN(EntityType.ZOMBIFIED_PIGLIN, GameEntityProperties(70, false)),

    BEE(EntityType.BEE, GameEntityProperties(50, false)),
    PARROT(EntityType.PARROT, GameEntityProperties(50, false)),
    VEX(EntityType.VEX, GameEntityProperties(75, false)),
    ALLAY(EntityType.ALLAY, GameEntityProperties(50, false)),

    IRON_GOLEM(EntityType.IRON_GOLEM, GameEntityProperties(350, false, "win_monster")),

    HORSE(EntityType.HORSE, GameEntityProperties(100, true)),
    ZOMBIE_HORSE(EntityType.ZOMBIE_HORSE, GameEntityProperties(100, true)),
    SKELETON_HORSE(EntityType.SKELETON_HORSE, GameEntityProperties(100, true)),

    MULE(EntityType.MULE, GameEntityProperties(70, false)),
    DONKEY(EntityType.DONKEY, GameEntityProperties(70, false)),

    LLAMA(EntityType.LLAMA, GameEntityProperties(70, false)),
    TRADER_LLAMA(EntityType.TRADER_LLAMA, GameEntityProperties(100, false)),

    POLAR_BEAR(EntityType.POLAR_BEAR, GameEntityProperties(100, true)),
    PANDA(EntityType.PANDA, GameEntityProperties(100, true)),
    CAMEL(EntityType.CAMEL, GameEntityProperties(100, true)),
    SNIFFER(EntityType.SNIFFER, GameEntityProperties(100, true)),

    COD(EntityType.COD, GameEntityProperties(40, false)),
    SALMON(EntityType.SALMON, GameEntityProperties(40, false)),
    TROPICAL_FISH(EntityType.TROPICAL_FISH, GameEntityProperties(40, false)),
    DOLPHIN(EntityType.DOLPHIN, GameEntityProperties(100, false)),
    GLOW_SQUID(EntityType.GLOW_SQUID, GameEntityProperties(60, false)),
    TADPOLE(EntityType.TADPOLE, GameEntityProperties(30, false)),

    BREEZE(EntityType.BREEZE, GameEntityProperties(100, false)),

    WARDEN(EntityType.WARDEN, GameEntityProperties(15000, false, "master_eliminator"));

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
        if (type == null) Bukkit.getConsoleSender().sendMessage(Component.text("There is no spawn egg with the name $typeName", NamedTextColor.DARK_RED))
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
        player.translateMessage("blocko.entity_shop.successfully_bought_entity_type",
            Placeholder.parsed("entity_type_name", getCorrectedTypeName()),
            Placeholder.parsed("amount", NumberUtils.format(properties.price)))
    }

}