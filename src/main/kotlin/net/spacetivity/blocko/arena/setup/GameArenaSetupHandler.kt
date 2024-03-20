package net.spacetivity.blocko.arena.setup

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.GameArenaStatus
import net.spacetivity.blocko.extensions.translateMessage
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.field.GameFieldProperties
import net.spacetivity.blocko.field.PathFace
import net.spacetivity.blocko.team.GameTeam
import net.spacetivity.blocko.team.GameTeamLocation
import net.spacetivity.blocko.utils.LocationUtils
import net.spacetivity.blocko.utils.MetadataUtils
import net.spacetivity.blocko.utils.ScoreboardUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.block.BlockFace
import org.bukkit.entity.*
import org.bukkit.scheduler.BukkitTask
import org.bukkit.scoreboard.Team
import java.util.*

class GameArenaSetupHandler {

    private val arenaSetupCache: MutableMap<UUID, GameArenaSetupData> = mutableMapOf()

    private val garageFieldScoreboardTeam: Team = ScoreboardUtils.registerScoreboardTeam("garage_field_setup", NamedTextColor.LIGHT_PURPLE)
    private val fieldScoreboardTeam: Team = ScoreboardUtils.registerScoreboardTeam("garage_field_setup", NamedTextColor.GREEN)
    private val turnScoreboardTeam: Team = ScoreboardUtils.registerScoreboardTeam("turn_setup", NamedTextColor.YELLOW)

    private var setupTask: BukkitTask? = null

    init {
        this.setupTask = Bukkit.getScheduler().runTaskTimer(BlockoGame.instance, Runnable {
            for (entry: MutableMap.MutableEntry<UUID, GameArenaSetupData> in this.arenaSetupCache.entries) {
                val player: Player = Bukkit.getPlayer(entry.key) ?: continue
                val arenaSetupData: GameArenaSetupData = entry.value
                val toolMode: GameArenaSetupTool.ToolMode = arenaSetupData.setupTool.currentMode

                if (toolMode == GameArenaSetupTool.ToolMode.SET_TURN || toolMode == GameArenaSetupTool.ToolMode.SET_TEAM_ENTRANCE) {
                    val facing: BlockFace = player.facing
                    if (facing == BlockFace.NORTH || facing == BlockFace.SOUTH || facing == BlockFace.EAST || facing == BlockFace.WEST)
                        player.translateMessage("blocko.setup.turn_direction", Placeholder.parsed("face", facing.name))
                }

                if (System.currentTimeMillis() < arenaSetupData.timeoutTimestamp) continue
                handleSetupEnd(player, false)
            }
        }, 0L, 20L)
    }

    fun stopTask() {
        if (this.setupTask == null) return
        this.setupTask!!.cancel()
        this.setupTask = null
    }

    fun getSetupData(uuid: UUID): GameArenaSetupData? {
        return this.arenaSetupCache[uuid]
    }

    fun startSetup(player: Player, arenaId: String) {
        if (hasOpenSetup(player.uniqueId)) {
            player.translateMessage("blocko.setup.already_in_setup_mode")
            return
        }

        if (this.arenaSetupCache.entries.any { it.value.arenaId.equals(arenaId, true) }) {
            player.translateMessage("blocko.setup.arena_already_configurated_by_player")
            return
        }

        val setupTool = GameArenaSetupTool(player)
        setupTool.setToPlayer()

        player.translateMessage("blocko.setup.setup_mode_activated")

        val arenaSetupData = GameArenaSetupData(arenaId, setupTool)
        arenaSetupData.gameTeams.addAll(
            listOf(
                GameTeam("red", NamedTextColor.RED, 0),
                GameTeam("blue", NamedTextColor.BLUE, 1),
                GameTeam("yellow", NamedTextColor.YELLOW, 2),
                GameTeam("green", NamedTextColor.GREEN, 3),
            )
        )

        this.arenaSetupCache[player.uniqueId] = arenaSetupData
    }

    fun handleSetupEnd(player: Player, success: Boolean) {
        if (!hasOpenSetup(player.uniqueId)) {
            player.translateMessage("blocko.setup.not_in_setup_mode")
            return
        }

        val arenaSetupData: GameArenaSetupData = this.arenaSetupCache[player.uniqueId]!!

        if (success) {
            if (!hasConfiguredFieldsAlready(player.uniqueId)) {
                player.translateMessage("blocko.setup.no_fields_configured")
                return
            }

            if (!hasConfiguredAllGarageFields(player.uniqueId)) {
                player.translateMessage("blocko.setup.no_garage_fields_configured")
                return
            }

            if (!hasConfiguredAllTeamSpawns(player.uniqueId)) {
                player.translateMessage("blocko.setup.not_enough_team_spawns_configured")
                return
            }

            BlockoGame.instance.gameArenaHandler.updateArenaStatus(arenaSetupData.arenaId, GameArenaStatus.READY)
            BlockoGame.instance.gameFieldHandler.initFields(arenaSetupData.gameFields)
            BlockoGame.instance.gameTeamHandler.initTeamSpawns(arenaSetupData.gameTeamLocations)
            BlockoGame.instance.gameArenaSignHandler.loadArenaSigns()
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

        player.inventory.remove(arenaSetupData.setupTool.itemStack)
        player.translateMessage("blocko.setup.setup_mode_deactivated")
        this.arenaSetupCache.remove(player.uniqueId)
    }

    fun addTeamSpawn(player: Player, teamName: String, location: Location) {
        if (!hasOpenSetup(player.uniqueId)) {
            player.translateMessage("blocko.setup.not_in_setup_mode")
            return
        }

        val arenaSetupData: GameArenaSetupData = this.arenaSetupCache[player.uniqueId]!!

        if (arenaSetupData.gameTeamLocations.any { it.x == location.x && it.y == location.y && it.z == location.z }) {
            player.translateMessage("blocko.setup.team_spawn_already_set")
            return
        }

        val centeredLocation: Location = LocationUtils.centerLocation(location)

        val yLevel: Double = BlockoGame.instance.gameArenaHandler.getArena(arenaSetupData.arenaId)!!.yLevel

        val teamSpawn = GameTeamLocation(
            arenaSetupData.arenaId,
            teamName,
            location.world.name,
            centeredLocation.x,
            yLevel,
            centeredLocation.z,
            location.yaw,
            location.pitch,
            false
        )

        arenaSetupData.gameTeamLocations.add(teamSpawn)
        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5F, 1.0F)
        player.translateMessage("blocko.setup.team_spawn_added")
    }

    fun addField(player: Player, location: Location) {
        if (!hasOpenSetup(player.uniqueId)) {
            player.translateMessage("blocko.setup.not_in_setup_mode")
            return
        }

        val arenaSetupData: GameArenaSetupData = this.arenaSetupCache[player.uniqueId]!!
        val x: Double = location.x
        val z: Double = location.z

        if (arenaSetupData.gameFields.any { it.x == x && it.z == z }) {
            player.translateMessage("blocko.setup.game_field_already_set")
            return
        }

        arenaSetupData.gameFields.add(
            GameField(
                arenaSetupData.arenaId,
                location.world,
                x,
                z,
                GameFieldProperties(mutableMapOf(), null, null, null),
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
        MetadataUtils.apply(displayEntity, "displayEntity", arenaSetupData.arenaId)

        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5F, 1.0F)
        player.translateMessage("blocko.setup.game_field_set",
            Placeholder.parsed("field_id", (arenaSetupData.gameFields.size - 1).toString()),
            Placeholder.parsed("x", x.toString()),
            Placeholder.parsed("z", z.toString()))
    }

    fun setTurn(player: Player, gameField: GameField, blockLocation: Location, face: PathFace) {
        if (!hasOpenSetup(player.uniqueId)) {
            player.translateMessage("blocko.setup.not_in_setup_mode")
            return
        }

        if (gameField.isGarageField) {
            player.translateMessage("blocko.setup.turn_not_creatable_at_garage_field")
            return
        }

        gameField.properties.rotation = face

        val entityLocation: Location = blockLocation.block.location.clone().toCenterLocation()
        val displayEntity: MagmaCube? = entityLocation.world.entities.find { it.world == entityLocation.world && it.location.x == entityLocation.x && it.location.z == entityLocation.z && it.type == EntityType.MAGMA_CUBE } as MagmaCube?

        if (displayEntity != null) {
            this.fieldScoreboardTeam.removeEntity(displayEntity)
            this.turnScoreboardTeam.addEntity(displayEntity)
        }

        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5F, 1.0F)
        player.translateMessage("blocko.setup.turning_point_created", Placeholder.parsed("face", face.name))
    }

    fun addGarageField(player: Player, teamName: String, location: Location) {
        if (!hasOpenSetup(player.uniqueId)) {
            player.translateMessage("blocko.setup.not_in_setup_mode")
            return
        }

        val arenaSetupData: GameArenaSetupData = this.arenaSetupCache[player.uniqueId]!!

        val x: Double = location.x
        val z: Double = location.z

        val possibleField: GameField? = arenaSetupData.gameFields.find { it.world == location.world && it.x == x && it.z == z }

        if (possibleField == null) {
            player.translateMessage("blocko.setup.no_field_found_at_location")
            return
        }

        if (possibleField.isGarageField) {
            player.translateMessage("blocko.setup.field_already_a_garage_field")
            return
        }

        if (possibleField.properties.rotation != null) {
            player.translateMessage("blocko.setup.field_not_able_for_garage_field")
            return
        }

        possibleField.isGarageField = true
        possibleField.properties.garageForTeam = teamName

        val entityLocation: Location = location.block.location.clone().toCenterLocation()
        val displayEntity: MagmaCube? = entityLocation.world.entities.find { it.world == entityLocation.world && it.location.x == entityLocation.x && it.location.z == entityLocation.z && it.type == EntityType.MAGMA_CUBE } as MagmaCube?

        if (displayEntity != null) {
            this.fieldScoreboardTeam.removeEntity(displayEntity)
            this.garageFieldScoreboardTeam.addEntity(displayEntity)
        } else {
            player.translateMessage("blocko.setup.cannot_update_entity_display")
        }

        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5F, 1.0F)
        player.translateMessage("blocko.setup.game_field_set_to_garage_field",
            Placeholder.parsed("x", x.toString()),
            Placeholder.parsed("z", z.toString()),
            Placeholder.parsed("team_color", "<${arenaSetupData.gameTeams.find { it.name == teamName }!!.color.asHexString()}>"),
            Placeholder.parsed("team_name", teamName))
    }

    fun setFieldId(player: Player, teamName: String, location: Location) {
        if (!hasOpenSetup(player.uniqueId)) {
            player.translateMessage("blocko.setup.not_in_setup_mode")
            return
        }

        val arenaSetupData: GameArenaSetupData = this.arenaSetupCache[player.uniqueId]!!

        val x: Double = location.x
        val z: Double = location.z

        val possibleField: GameField? = arenaSetupData.gameFields.find { it.world == location.world && it.x == x && it.z == z }

        if (possibleField == null) {
            player.translateMessage("blocko.setup.not_in_setup_mode")
            return
        }

        if (possibleField.properties.getFieldId(teamName) != null) {
            player.translateMessage("blocko.setup.field_already_has_id")
            return
        }

        val currentFieldIndex: Int = arenaSetupData.setupTool.fieldIndex
        possibleField.properties.setFieldId(teamName, currentFieldIndex)
        arenaSetupData.setupTool.fieldIndex += 1

        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5F, 1.0F)
        player.translateMessage("blocko.setup.set_field_id_for_team",
            Placeholder.parsed("field_id", currentFieldIndex.toString()),
            Placeholder.parsed("team_color", "<${arenaSetupData.gameTeams.find { it.name == teamName }!!.color.asHexString()}>"),
            Placeholder.parsed("team_name", teamName))
    }

    private fun hasOpenSetup(uuid: UUID): Boolean {
        return this.arenaSetupCache[uuid] != null
    }

    private fun hasConfiguredFieldsAlready(uuid: UUID): Boolean {
        val arenaSetupData: GameArenaSetupData = getSetupData(uuid) ?: return false
        return arenaSetupData.gameFields.isNotEmpty()
    }

    private fun hasConfiguredAllGarageFields(uuid: UUID): Boolean {
        val arenaSetupData: GameArenaSetupData = getSetupData(uuid) ?: return false
        return arenaSetupData.gameFields.filter { it.isGarageField }.size == 16
    }

    private fun hasConfiguredAllTeamSpawns(uuid: UUID): Boolean {
        val arenaSetupData: GameArenaSetupData = getSetupData(uuid) ?: return false
        return arenaSetupData.gameTeamLocations.size == 16
    }

}