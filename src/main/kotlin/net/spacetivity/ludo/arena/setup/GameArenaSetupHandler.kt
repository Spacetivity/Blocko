package net.spacetivity.ludo.arena.setup

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArenaStatus
import net.spacetivity.ludo.field.GameField
import net.spacetivity.ludo.field.GameFieldProperties
import net.spacetivity.ludo.team.GameTeam
import net.spacetivity.ludo.team.GameTeamLocation
import net.spacetivity.ludo.utils.MetadataUtils
import net.spacetivity.ludo.utils.PathFace
import net.spacetivity.ludo.utils.ScoreboardUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.*
import org.bukkit.scoreboard.Team
import java.time.Duration
import java.util.*

class GameArenaSetupHandler {

    private val arenaSetupCache: Cache<UUID, GameArenaSetupData> = CacheBuilder.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(3))
        .removalListener<UUID, GameArenaSetupData> {
            val player: Player = Bukkit.getPlayer(it.key!!) ?: return@removalListener
            handleSetupEnd(player, false)
        }
        .build()

    private val garageFieldScoreboardTeam: Team = ScoreboardUtils.registerScoreboardTeam("garage_field_setup", NamedTextColor.LIGHT_PURPLE)
    private val fieldScoreboardTeam: Team = ScoreboardUtils.registerScoreboardTeam("garage_field_setup", NamedTextColor.GREEN)
    private val turnScoreboardTeam: Team = ScoreboardUtils.registerScoreboardTeam("turn_setup", NamedTextColor.YELLOW)

    fun getSetupData(uuid: UUID): GameArenaSetupData? {
        return this.arenaSetupCache.getIfPresent(uuid)
    }

    fun startSetup(player: Player, arenaId: String) {
        if (hasOpenSetup(player.uniqueId)) {
            player.sendMessage(Component.text("You are already in setup mode!"))
            return
        }

        val setupTool = GameArenaSetupTool(player)
        setupTool.setToPlayer()

        player.sendMessage(Component.text("You are now in setup mode!"))

        val arenaSetupData = GameArenaSetupData(arenaId, setupTool)
        arenaSetupData.gameTeams.addAll(
            listOf(
                GameTeam("red", NamedTextColor.RED, 0),
                GameTeam("green", NamedTextColor.GREEN, 1),
                GameTeam("blue", NamedTextColor.BLUE, 2),
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
        player.inventory.remove(arenaSetupData.setupTool.itemStack)
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

        val oldTeamEntrance: GameField? = arenaSetupData.gameFields.find { it.x == x && it.z == z && it.properties.teamEntrance != null && it.properties.teamEntrance.equals(teamName, true) }

        if (oldTeamEntrance != null)
            oldTeamEntrance.properties.teamEntrance = null

        gameField.properties.teamEntrance = teamName
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
                arenaSetupData.arenaId,
                location.world,
                x,
                z,
                GameFieldProperties(mutableMapOf(), null, null),
                false,
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
        player.sendActionBar(Component.text("Game field #${arenaSetupData.gameFields.size - 1} at (${x} | ${z}) added."))
    }

    fun setTurn(player: Player, gameField: GameField, blockLocation: Location, face: PathFace) {
        if (!hasOpenSetup(player.uniqueId)) {
            player.sendMessage(Component.text("You are not in setup mode!"))
            return
        }

        if (gameField.isGarageField) {
            player.sendMessage(Component.text("You cannot set a turn to a garage field!"))
            return
        }

        gameField.properties.turnComponent = face

        val entityLocation: Location = blockLocation.block.location.clone().toCenterLocation()
        val displayEntity: MagmaCube? = blockLocation.world.entities.find { it.world == entityLocation.world && it.location.x == entityLocation.x && it.location.y == blockLocation.y && it.location.z == blockLocation.z && it.type == EntityType.MAGMA_CUBE } as MagmaCube?

        if (displayEntity != null) {
            this.fieldScoreboardTeam.removeEntity(displayEntity)
            this.turnScoreboardTeam.addEntity(displayEntity)
        } else {
            player.sendMessage(Component.text("Cannot update entity display!", NamedTextColor.DARK_RED))
        }

        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5F, 1.0F)
        player.sendMessage(Component.text("You created a turning point for your field. (Direction: ${face.name})"))
    }

    fun addGarageField(player: Player, teamName: String, location: Location) {
        if (!hasOpenSetup(player.uniqueId)) {
            player.sendMessage(Component.text("You are not in setup mode!"))
            return
        }

        val arenaSetupData: GameArenaSetupData = this.arenaSetupCache.getIfPresent(player.uniqueId)!!

        val x: Double = location.x
        val z: Double = location.z

        val possibleField: GameField? = arenaSetupData.gameFields.find { it.world == location.world && it.x == x && it.z == z && it.isGarageField }

        if (possibleField == null) {
            player.sendMessage(Component.text("There is already a garage field on this location!"))
            return
        }

        if (possibleField.properties.teamEntrance != null || possibleField.properties.turnComponent != null) {
            player.sendMessage(Component.text("This field cannot be a garage field!"))
            return
        }

        possibleField.isGarageField = true

        val entityLocation: Location = location.block.location.clone().toCenterLocation()
        val displayEntity: MagmaCube? = location.world.entities.find { it.world == entityLocation.world && it.location.x == entityLocation.x && it.location.y == location.y && it.location.z == location.z && it.type == EntityType.MAGMA_CUBE } as MagmaCube?

        if (displayEntity != null) {
            this.fieldScoreboardTeam.removeEntity(displayEntity)
            this.garageFieldScoreboardTeam.addEntity(displayEntity)
        } else {
            player.sendMessage(Component.text("Cannot update entity display!", NamedTextColor.DARK_RED))
        }

        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5F, 1.0F)
        player.sendMessage(Component.text("Set field at (${x} | ${z}) to garage field of team ${teamName}."))
    }

    fun setFieldId(player: Player, teamName: String, location: Location) {
        if (!hasOpenSetup(player.uniqueId)) {
            player.sendMessage(Component.text("You are not in setup mode!"))
            return
        }

        val arenaSetupData: GameArenaSetupData = this.arenaSetupCache.getIfPresent(player.uniqueId)!!

        val x: Double = location.x
        val z: Double = location.z

        val possibleField: GameField? = arenaSetupData.gameFields.find { it.world == location.world && it.x == x && it.z == z && it.isGarageField }

        if (possibleField == null) {
            player.sendMessage(Component.text("There is already a garage field on this location!"))
            return
        }

        val newFieldIndex: Int = arenaSetupData.setupTool.fieldIndex.inc()
        possibleField.properties.setFieldId(teamName, newFieldIndex)

        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5F, 1.0F)
        player.sendMessage(Component.text("Set field id to $newFieldIndex for team $teamName", NamedTextColor.YELLOW))
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
        return arenaSetupData.gameFields.filter { it.isGarageField }.size == 16
    }

    private fun hasConfiguredAllTeamEntrances(uuid: UUID): Boolean {
        val arenaSetupData: GameArenaSetupData = getSetupData(uuid) ?: return false
        return arenaSetupData.gameFields.filter { it.properties.teamEntrance != null }.size == 4
    }

    private fun hasConfiguredAllTeamSpawns(uuid: UUID): Boolean {
        val arenaSetupData: GameArenaSetupData = getSetupData(uuid) ?: return false
        return arenaSetupData.gameTeamLocations.size == 16
    }

}