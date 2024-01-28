package net.spacetivity.ludo.arena.setup

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArenaStatus
import net.spacetivity.ludo.field.GameField
import net.spacetivity.ludo.garageField.GameGarageField
import net.spacetivity.ludo.team.GameTeam
import net.spacetivity.ludo.team.GameTeamLocation
import net.spacetivity.ludo.utils.ItemUtils
import net.spacetivity.ludo.utils.MetadataUtils
import net.spacetivity.ludo.utils.ScoreboardUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.*
import org.bukkit.scoreboard.Team
import java.time.Duration
import java.util.*

class GameArenaSetupHandler {

    private val arenaSetupCache: Cache<UUID, GameArenaSetupData> = CacheBuilder.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(10))
        .build()

    private val garageFieldScoreboardTeam: Team = ScoreboardUtils.registerScoreboardTeam("garage_field_setup", NamedTextColor.LIGHT_PURPLE)
    val fieldScoreboardTeam: Team = ScoreboardUtils.registerScoreboardTeam("garage_field_setup", NamedTextColor.GREEN)
    val turnScoreboardTeam: Team = ScoreboardUtils.registerScoreboardTeam("turn_setup", NamedTextColor.YELLOW)

    fun getSetupData(uuid: UUID): GameArenaSetupData? {
        return this.arenaSetupCache.getIfPresent(uuid)
    }

    fun startSetup(player: Player, arenaId: String) {
        if (hasOpenSetup(player.uniqueId)) {
            player.sendMessage(Component.text("You are already in setup mode!"))
            return
        }

        player.inventory.setItem(0, ItemUtils(Material.IRON_HOE)
            .setName(Component.text("Left-click: Set Turn | Right-click: Add field"))
            .build())

        player.inventory.setItem(1, ItemUtils(Material.GOLDEN_HOE)
            .setName(Component.text("Left-click: Set Team Entrance | Right-click: Add team garage field"))
            .build())

        player.sendMessage(Component.text("You are now in setup mode!"))

        val arenaSetupData = GameArenaSetupData(arenaId)
        arenaSetupData.gameTeams.addAll(
            listOf(
                GameTeam("red", NamedTextColor.RED, 0),
                GameTeam("green", NamedTextColor.GREEN, 1),
                GameTeam("blue", NamedTextColor.BLUE,2),
                GameTeam("yellow", NamedTextColor.YELLOW, 3),
            )
        )

        this.arenaSetupCache.put(player.uniqueId, arenaSetupData)
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

            if (!hasConfiguredAllGarageFields(player.uniqueId)) {
                player.sendMessage(Component.text("You have to add all garage fields for all teams before you finish the setup!"))
                return
            }

            if (!hasConfiguredAllTeamEntrances(player.uniqueId)) {
                player.sendMessage(Component.text("You have to set all team entrances before you finish the setup!"))
                return
            }

            if (!hasConfiguredAllTeamSpawns(player.uniqueId)) {
                player.sendMessage(Component.text("You have to set all team spawn locations before you finish the setup!"))
                return
            }

            LudoGame.instance.gameArenaHandler.updateArenaStatus(arenaSetupData.arenaId, GameArenaStatus.READY)
            LudoGame.instance.gameFieldHandler.initFields(arenaSetupData.gameFields)
            LudoGame.instance.gameGarageFieldHandler.initGarageFields(arenaSetupData.gameGarageFields)
            LudoGame.instance.gameTeamHandler.initTeamSpawns(arenaSetupData.gameTeamLocations)
            LudoGame.instance.gameArenaSignHandler.loadArenaSigns()
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

        MetadataUtils.remove(player, "fieldsFinished")
        player.inventory.remove(Material.IRON_HOE)
        player.inventory.remove(Material.GOLDEN_HOE)
        player.sendMessage(Component.text("You are no longer in setup mode!"))
        this.arenaSetupCache.invalidate(player.uniqueId)
    }

    fun setTeamEntrance(player: Player, teamName: String, location: Location) {
        if (!hasOpenSetup(player.uniqueId)) {
            player.sendMessage(Component.text("You are not in setup mode!"))
            return
        }

        val x: Double = location.x
        val z: Double = location.z

        val arenaSetupData: GameArenaSetupData = this.arenaSetupCache.getIfPresent(player.uniqueId)!!
        val gameField: GameField? = arenaSetupData.gameFields.find { it.x == x && it.z == z }

        if (gameField == null) {
            player.sendMessage(Component.text("There is no game field at this location!"))
            return
        }

        val oldTeamEntrance: GameField? = arenaSetupData.gameFields.find { it.x == x && it.z == z && it.teamGarageEntrance.equals(teamName, true) }

        if (oldTeamEntrance != null)
            oldTeamEntrance.teamGarageEntrance = null

        gameField.teamGarageEntrance = teamName
        player.sendMessage(Component.text("Team entrance for team $teamName created."))
        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5F, 1.0F)
    }

    fun addTeamSpawn(player: Player, teamName: String, location: Location) {
        if (!hasOpenSetup(player.uniqueId)) {
            player.sendMessage(Component.text("You are not in setup mode!"))
            return
        }

        val arenaSetupData: GameArenaSetupData = this.arenaSetupCache.getIfPresent(player.uniqueId)!!

        if (arenaSetupData.gameTeamLocations.any { it.x == location.x && it.y == location.y && it.z == location.z }) {
            player.sendMessage(Component.text("There is already a team spawn on this location!"))
            return
        }

        val teamSpawn = GameTeamLocation(
            arenaSetupData.arenaId,
            teamName,
            location.world.name,
            location.x,
            location.y,
            location.z,
            location.yaw,
            location.pitch,
            false
        )

        arenaSetupData.gameTeamLocations.add(teamSpawn)
        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5F, 1.0F)
        player.sendMessage(Component.text("Team Spawn added."))
    }

    fun addField(player: Player, location: Location) {
        if (!hasOpenSetup(player.uniqueId)) {
            player.sendMessage(Component.text("You are not in setup mode!"))
            return
        }

        if (MetadataUtils.has(player, "fieldsFinished")) {
            player.sendMessage(Component.text("All fields are already set for this arena!"))
            return
        }

        val arenaSetupData: GameArenaSetupData = this.arenaSetupCache.getIfPresent(player.uniqueId)!!
        val fieldId: Int = arenaSetupData.gameFields.size

        val x: Double = location.x
        val z: Double = location.z

        if (arenaSetupData.gameFields.any { it.x == x && it.z == z }) {
            val firstField = arenaSetupData.gameFields[0]
            if (firstField.x == x && firstField.z == z && arenaSetupData.gameFields.any { it.x != x && it.z != z }) {

                for (displayEntity: LivingEntity in location.world.entities.filterIsInstance<LivingEntity>()) {
                    if (!MetadataUtils.has(displayEntity, "displayEntity")) continue
                    val entityArenaId: String = MetadataUtils.get(displayEntity, "displayEntity") ?: continue
                    if (entityArenaId != arenaSetupData.arenaId) continue

                    this.fieldScoreboardTeam.addEntity(displayEntity)
                }

                MetadataUtils.set(player, "fieldsFinished", arenaSetupData.arenaId)
                player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5F, 1.0F)
                player.sendMessage(Component.text("Fields are now set and connected!."))
                return
            }

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
                null,
                false
            )
        )

        val entityLocation: Location = location.block.location.clone().toCenterLocation()
        val displayEntity: MagmaCube = location.world.spawnEntity(entityLocation, EntityType.MAGMA_CUBE) as MagmaCube

        displayEntity.isInvisible = true
        displayEntity.size = 1
        displayEntity.isSilent = true
        displayEntity.isInvulnerable = true
        displayEntity.isGlowing = true
        displayEntity.setAI(false)
        displayEntity.setGravity(false)
        MetadataUtils.set(displayEntity, "displayEntity", arenaSetupData.arenaId)

        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5F, 1.0F)
        player.sendMessage(Component.text("Game field #${arenaSetupData.gameFields.size - 1} at (${x} | ${z}) added."))
    }

    fun addGarageField(player: Player, teamName: String, location: Location) {
        if (!hasOpenSetup(player.uniqueId)) {
            player.sendMessage(Component.text("You are not in setup mode!"))
            return
        }

        val arenaSetupData: GameArenaSetupData = this.arenaSetupCache.getIfPresent(player.uniqueId)!!
        val fieldId: Int = arenaSetupData.gameGarageFields.size

        val x: Double = location.x
        val z: Double = location.z

        if (arenaSetupData.gameGarageFields.any { it.x == x && it.z == z }) {
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BASS, 0.1F, 0.1F)
            player.sendMessage(Component.text("There is already a garage field on this location!"))
            return
        }

        arenaSetupData.gameGarageFields.add(
            GameGarageField(
                fieldId,
                arenaSetupData.arenaId,
                teamName,
                location.world,
                x,
                z
            )
        )

        val entityLocation: Location = location.block.location.clone().toCenterLocation()
        val displayEntity: MagmaCube = location.world.spawnEntity(entityLocation, EntityType.MAGMA_CUBE) as MagmaCube

        displayEntity.isInvisible = true
        displayEntity.size = 1
        displayEntity.isSilent = true
        displayEntity.isInvulnerable = true
        displayEntity.isGlowing = true
        displayEntity.setAI(false)
        displayEntity.setGravity(false)
        MetadataUtils.set(displayEntity, "displayEntity", arenaSetupData.arenaId)

        this.garageFieldScoreboardTeam.addEntity(displayEntity)

        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5F, 1.0F)
        player.sendMessage(Component.text("Game Garage field #${arenaSetupData.gameFields.size - 1} at (${x} | ${z}) added."))
    }

    fun hasOpenSetup(uuid: UUID): Boolean {
        return this.arenaSetupCache.getIfPresent(uuid) != null
    }

    fun hasConfiguredFieldsAlready(uuid: UUID): Boolean {
        val arenaSetupData: GameArenaSetupData = getSetupData(uuid) ?: return false
        return arenaSetupData.gameFields.isNotEmpty()
    }

    private fun hasConfiguredAllGarageFields(uuid: UUID): Boolean {
        val arenaSetupData: GameArenaSetupData = getSetupData(uuid) ?: return false
        return arenaSetupData.gameGarageFields.size == 16
    }

    private fun hasConfiguredAllTeamEntrances(uuid: UUID): Boolean {
        val arenaSetupData: GameArenaSetupData = getSetupData(uuid) ?: return false
        return arenaSetupData.gameFields.filter { it.teamGarageEntrance != null }.size == 4
    }

    private fun hasConfiguredAllTeamSpawns(uuid: UUID): Boolean {
        val arenaSetupData: GameArenaSetupData = getSetupData(uuid) ?: return false
        return arenaSetupData.gameTeamLocations.size == 16
    }

}