package net.spacetivity.ludo.arena.setup

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
        this.setupTask = Bukkit.getScheduler().runTaskTimer(LudoGame.instance, Runnable {
            for (entry: MutableMap.MutableEntry<UUID, GameArenaSetupData> in this.arenaSetupCache.entries) {
                val player: Player = Bukkit.getPlayer(entry.key) ?: continue
                val arenaSetupData: GameArenaSetupData = entry.value
                val toolMode: GameArenaSetupTool.ToolMode = arenaSetupData.setupTool.currentMode

                if (toolMode == GameArenaSetupTool.ToolMode.SET_TURN || toolMode == GameArenaSetupTool.ToolMode.SET_TEAM_ENTRANCE) {
                    val facing: BlockFace = player.facing
                    if (facing == BlockFace.NORTH || facing == BlockFace.SOUTH || facing == BlockFace.EAST || facing == BlockFace.WEST) {
                        player.sendActionBar(Component.text("Current facing direction: ${facing.name}", NamedTextColor.GRAY))
                    }
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
            player.sendMessage(Component.text("You are already in setup mode!"))
            return
        }

        if (this.arenaSetupCache.entries.any { it.value.arenaId.equals(arenaId, true) }) {
            player.sendMessage(Component.text("This arena is already being configured.", NamedTextColor.RED))
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

        val arenaSetupData: GameArenaSetupData = this.arenaSetupCache[player.uniqueId]!!

        if (success) {
            if (!hasConfiguredFieldsAlready(player.uniqueId)) {
                player.sendMessage(Component.text("You have to add game-fields first before you finish the setup!"))
                return
            }

            if (!hasConfiguredAllGarageFields(player.uniqueId)) {
                player.sendMessage(Component.text("You have to add all garage fields for all teams before you finish the setup!"))
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

        player.inventory.remove(arenaSetupData.setupTool.itemStack)
        player.sendMessage(Component.text("You are no longer in setup mode!"))
        this.arenaSetupCache.remove(player.uniqueId)
    }

    fun addTeamSpawn(player: Player, teamName: String, location: Location) {
        if (!hasOpenSetup(player.uniqueId)) {
            player.sendMessage(Component.text("You are not in setup mode!"))
            return
        }

        val arenaSetupData: GameArenaSetupData = this.arenaSetupCache[player.uniqueId]!!

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

        val arenaSetupData: GameArenaSetupData = this.arenaSetupCache[player.uniqueId]!!
        val x: Double = location.x
        val z: Double = location.z

        if (arenaSetupData.gameFields.any { it.x == x && it.z == z }) {
            player.sendMessage(Component.text("There is already a game field at this location!", NamedTextColor.RED))
            return
        }

        arenaSetupData.gameFields.add(
            GameField(
                arenaSetupData.arenaId,
                location.world,
                x,
                z,
                GameFieldProperties(mutableMapOf(), null, null,null),
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

        gameField.properties.rotation = face

        val entityLocation: Location = blockLocation.block.location.clone().toCenterLocation()
        val displayEntity: MagmaCube? = entityLocation.world.entities.find { it.world == entityLocation.world && it.location.x == entityLocation.x && it.location.z == entityLocation.z && it.type == EntityType.MAGMA_CUBE } as MagmaCube?

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

        val arenaSetupData: GameArenaSetupData = this.arenaSetupCache[player.uniqueId]!!

        val x: Double = location.x
        val z: Double = location.z

        val possibleField: GameField? = arenaSetupData.gameFields.find { it.world == location.world && it.x == x && it.z == z }

        if (possibleField == null) {
            player.sendMessage(Component.text("There is no field on this location!"))
            return
        }

        if (possibleField.isGarageField) {
            player.sendMessage(Component.text("This field is already a garage field!"))
        }

        if (possibleField.properties.rotation != null) {
            player.sendMessage(Component.text("This field cannot be a garage field!"))
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

        val arenaSetupData: GameArenaSetupData = this.arenaSetupCache[player.uniqueId]!!

        val x: Double = location.x
        val z: Double = location.z

        val possibleField: GameField? = arenaSetupData.gameFields.find { it.world == location.world && it.x == x && it.z == z }

        if (possibleField == null) {
            player.sendMessage(Component.text("There is no field on this location!"))
            return
        }

        if (possibleField.properties.getFieldId(teamName) != null) {
            player.sendMessage(Component.text("This field already has an id!"))
            return
        }

        val currentFieldIndex: Int = arenaSetupData.setupTool.fieldIndex
        possibleField.properties.setFieldId(teamName, currentFieldIndex)
        arenaSetupData.setupTool.fieldIndex += 1

        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5F, 1.0F)
        player.sendMessage(Component.text("Set field id to $currentFieldIndex for team $teamName", NamedTextColor.YELLOW))
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