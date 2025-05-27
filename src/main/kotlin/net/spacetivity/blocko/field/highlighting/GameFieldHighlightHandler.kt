package net.spacetivity.blocko.field.highlighting

import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.arena.ArenaStatus
import net.spacetivity.blocko.arena.id.ArenaId
import net.spacetivity.blocko.field.highlighting.impl.*
import net.spacetivity.blocko.team.GameTeam
import net.spacetivity.blocko.utils.Constants
import net.spacetivity.blocko.utils.LocationUtils
import net.spacetivity.blocko.utils.ScoreboardTeamUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.MagmaCube
import org.bukkit.entity.Player
import java.util.*
import kotlin.reflect.KClass

class GameFieldHighlightHandler {

    val highlightEntities = mutableMapOf<UUID, HighlightEntity>()

    val highlightModes = mutableMapOf(
        Pair("game_field_highlight", GameFieldHighlightMode()),
        Pair("turning_point_highlight", TurningPointHighlightMode()),
        Pair("garage_field_highlight", GarageFieldHighlightMode()),
        Pair("game_entity_goal_field_highlight", GameEntityGoalFieldHighlightMode()),
    )

    init {
        for (gameTeam in Constants.GAME_TEAMS) {
            val teamSpawnHighlightMode = TeamSpawnHighlightMode(gameTeam)
            this.highlightModes.put(teamSpawnHighlightMode.teamName, teamSpawnHighlightMode)

            val teamPathHighlightMode = TeamPathHighlightMode(gameTeam)
            this.highlightModes.put(teamPathHighlightMode.teamName, teamPathHighlightMode)
        }
    }

    fun registerHighlightScoreboardTeams(player: Player) {
        for (highlightMode in this.highlightModes.values) {
            ScoreboardTeamUtils.registerScoreboardTeam(player.scoreboard, highlightMode.teamName, highlightMode.color)
        }
    }

    fun unregisterHighlightScoreboardTeams(player: Player) {
        for (team in player.scoreboard.teams) {
            if (!team.name.contains("highlight")) continue

            for (highlightMode in this.highlightModes.values) {
                val teamName = highlightMode.teamName

                for (highlightEntity in this.highlightEntities.values) {
                    if (highlightEntity.highlightMode.teamName != teamName) continue
                    val entity = Bukkit.getEntity(highlightEntity.uuid) ?: continue
                    team.removeEntity(entity)
                }
            }

            team.unregister()
        }
    }

    fun spawnOrUpdateHighlightEntity(arenaId: ArenaId, location: Location, highlightModeClass: KClass<out HighlightMode>) {
        val highlightMode = getHighlightModeByClass(highlightModeClass)
            ?: throw NullPointerException("Cannot find highlightMode $highlightModeClass")

        spawnOrUpdateHighlightEntity(arenaId, location, highlightMode)
    }

    fun spawnOrUpdateHighlightEntity(arenaId: ArenaId, location: Location, highlightMode: HighlightMode) {
        val block = location.block
        val blockLocation = LocationUtils.centerLocation(block.location)

        var displayEntity = getHighlightEntity(arenaId, blockLocation)

        val highlightEntity: HighlightEntity?

        if (displayEntity == null) {
            displayEntity = blockLocation.world.spawnEntity(blockLocation, EntityType.MAGMA_CUBE) as MagmaCube
            displayEntity.size = 2
            displayEntity.isGlowing = true
            displayEntity.isSilent = true
            displayEntity.isInvulnerable = true
            displayEntity.isCollidable = false
            displayEntity.isAggressive = false
            displayEntity.isAware = false
            displayEntity.isInvisible = true
            displayEntity.setGravity(false)
            displayEntity.setAI(false)

            highlightEntity = HighlightEntity(displayEntity.uniqueId, arenaId, highlightMode)
            this.highlightEntities[displayEntity.uniqueId] = highlightEntity
        } else {
            highlightEntity = this.highlightEntities[displayEntity.uniqueId]
        }

        if (highlightEntity == null) return

        val arena = Blocko.instance.arenaHandler.getArena(arenaId) ?: return
        val arenaPlayers = when (arena.status) {
            ArenaStatus.CONFIGURATING -> Blocko.instance.arenaSetupHandler.getSetupPlayers(arenaId)
            ArenaStatus.READY -> arena.getAllPlayers()
            else -> emptyList()
        }

        for (player in arenaPlayers) {
            val scoreboard = player.scoreboard

            for (team in scoreboard.teams) {
                if (!team.hasEntity(displayEntity)) continue
                team.removeEntity(displayEntity)
            }

            scoreboard.getTeam(highlightMode.teamName)?.addEntity(displayEntity)
        }
    }

    fun removeHighlightEntities(arenaId: ArenaId, vararg highlightModeClasses: KClass<out HighlightMode>) {
        val entityIdsToRemove = mutableListOf<UUID>()

        for (highlightEntity in this.highlightEntities.values) {
            val uuid = highlightEntity.uuid
            if (highlightEntity.arenaId != arenaId) continue
            if (!highlightModeClasses.contains(highlightEntity.highlightMode::class)) continue

            entityIdsToRemove.add(uuid)
        }

        val arena = Blocko.instance.arenaHandler.getArena(arenaId) ?: return
        val arenaPlayers = when (arena.status) {
            ArenaStatus.CONFIGURATING -> Blocko.instance.arenaSetupHandler.getSetupPlayers(arenaId)
            ArenaStatus.READY -> arena.getAllPlayers()
            else -> emptyList()
        }

        for (uuid in entityIdsToRemove) {
            val entity = Bukkit.getEntity(uuid) ?: continue

            for (player in arenaPlayers) {
                for (team in player.scoreboard.teams) {
                    if (!team.hasEntity(entity)) continue
                    team.removeEntity(entity)
                }
            }

            entity.remove()
            this.highlightEntities.remove(uuid)
        }
    }

    fun getHighlightEntity(arenaId: ArenaId, location: Location): MagmaCube? {
        var displayEntity: MagmaCube? = null

        for (highlightEntity in this.highlightEntities.values) {
            if (highlightEntity.arenaId != arenaId) continue

            val entity = Bukkit.getEntity(highlightEntity.uuid) ?: continue
            if (entity !is MagmaCube) continue
            if (entity.location != location) continue

            displayEntity = entity
        }

        return displayEntity
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : HighlightMode> getHighlightModeByTeam(gameTeam: GameTeam, clazz: KClass<T>): T? {
        val highlightModesByClass = this.highlightModes.values.filter { it::class.java.name.equals(clazz.java.name) }
        return highlightModesByClass.find { it.color.asHexString() == gameTeam.color.asHexString() } as T?
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : HighlightMode> getHighlightModeByBlockType(type: Material, clazz: KClass<T>): T? {
        if (!type.name.contains("red", true) && !type.name.contains("green", true) && !type.name.contains("blue", true) && !type.name.contains("yellow", true)) return null

        val blockTypeColorString = type.name.split("_")[0].lowercase()
        val highlightModesByClass = this.highlightModes.values.filter { it::class.java.name.equals(clazz.java.name) }

        var result: T? = null

        for (highlightMode in highlightModesByClass) {
            val highlightModeTeamPrefix = highlightMode.teamName.split("_")[0].lowercase()
            if (highlightModeTeamPrefix.isEmpty() || highlightModeTeamPrefix != blockTypeColorString) continue
            result = highlightMode as T?
        }

        return result
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : HighlightMode> getHighlightModeByClass(clazz: KClass<T>): T? {
        return this.highlightModes.values.find { it::class.java.name.equals(clazz.java.name) } as T?
    }

}