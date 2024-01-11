package net.spacetivity.ludo.arena.setup

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import net.kyori.adventure.text.Component
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArenaOption
import net.spacetivity.ludo.field.GameField
import net.spacetivity.ludo.utils.ItemUtils
import net.spacetivity.ludo.utils.MetadataUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.*
import java.time.Duration
import java.util.*

class GameArenaSetupHandler {

    private val arenaSetupCache: Cache<UUID, GameArenaSetupData> = CacheBuilder.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(2))
        .build()

    fun startSetup(player: Player, arenaId: String) {
        if (hasOpenSetup(player.uniqueId)) {
            player.sendMessage(Component.text("You are already in setup mode!"))
            return
        }

        player.inventory.setItem(0, ItemUtils(Material.IRON_HOE)
            .setName(Component.text("Left-click: Set Turn | Right-click: Add field"))
            .build())


        player.sendMessage(Component.text("You are now in setup mode!"))
        this.arenaSetupCache.put(player.uniqueId, GameArenaSetupData(arenaId))
    }

    fun handleSetupEnd(player: Player, success: Boolean) {
        if (!hasOpenSetup(player.uniqueId)) {
            player.sendMessage(Component.text("You are not in setup mode!"))
            return
        }

        val arenaSetupData: GameArenaSetupData = this.arenaSetupCache.getIfPresent(player.uniqueId)!!

        if (success) {
            if (!hasConfiguredFieldsAlready(player.uniqueId)) {
                player.sendMessage(Component.text("You have to add game-fields first before you finish the setup!"))
                return
            }

            LudoGame.instance.gameArenaHandler.updateArenaStatus(arenaSetupData.arenaId, GameArenaOption.Status.READY)
            LudoGame.instance.gameFieldHandler.initFields(arenaSetupData.gameFields)
            LudoGame.instance.gameTeamHandler.initTeamSpawns(arenaSetupData.gameTeamSpawns)
        }

        for (entities: MutableList<Entity> in Bukkit.getWorlds().map { it.entities }) {
            for (entity: Entity in entities) {
                if (entity !is LivingEntity) continue
                if (!entity.hasMetadata("displayEntity")) continue

                val arenaId: String = MetadataUtils.get(entity, "displayEntity")!!
                if (!arenaSetupData.arenaId.equals(arenaId, true)) continue

                entity.remove()
            }
        }

        player.inventory.remove(Material.IRON_HOE)
        player.sendMessage(Component.text("You are no longer in setup mode!"))
        this.arenaSetupCache.invalidate(player.uniqueId)
    }

    fun addField(player: Player, location: Location) {
        if (!hasOpenSetup(player.uniqueId)) {
            player.sendMessage(Component.text("You are not in setup mode!"))
            return
        }

        val arenaSetupData: GameArenaSetupData = this.arenaSetupCache.getIfPresent(player.uniqueId)!!
        val fieldId: Int = arenaSetupData.gameFields.size

        val x: Double = location.x
        val z: Double = location.z

        if (arenaSetupData.gameFields.any { it.x == x && it.z == z }) {
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BASS, 0.1F, 0.1F)
            player.sendMessage(Component.text("There is already a field on this location!"))
            return
        }

        arenaSetupData.gameFields.add(
            GameField(
                fieldId,
                arenaSetupData.arenaId,
                location.world,
                x,
                z,
                null,
                false
            )
        )

        val entityLocation: Location = location.block.location.clone()
        val y: Double = entityLocation.y

        val fixedEntityLocation = entityLocation.clone().toCenterLocation()
        fixedEntityLocation.y = y

        val displayEntity: MagmaCube = location.world.spawnEntity(fixedEntityLocation, EntityType.MAGMA_CUBE) as MagmaCube
        displayEntity.isInvisible = true
        displayEntity.size = 2
        displayEntity.isSilent = true
        displayEntity.isInvulnerable = true
        displayEntity.isGlowing = true
        displayEntity.setAI(false)
        displayEntity.setGravity(false)
        MetadataUtils.set(displayEntity, "displayEntity", arenaSetupData.arenaId)

        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5F, 1.0F)
        player.sendMessage(Component.text("Game field #${arenaSetupData.gameFields.size - 1} at (${x} | ${z}) added."))
    }

    fun hasOpenSetup(uuid: UUID): Boolean {
        return this.arenaSetupCache.getIfPresent(uuid) != null
    }

    fun hasConfiguredFieldsAlready(uuid: UUID): Boolean {
        return this.arenaSetupCache.getIfPresent(uuid)?.gameFields?.isNotEmpty() ?: false
    }

}