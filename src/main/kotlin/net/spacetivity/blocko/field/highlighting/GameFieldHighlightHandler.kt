package net.spacetivity.blocko.field.highlighting

import net.spacetivity.blocko.arena.id.ArenaId
import net.spacetivity.blocko.field.highlighting.scoreboard.HighlightMode
import net.spacetivity.blocko.field.highlighting.scoreboard.impl.*
import net.spacetivity.blocko.team.GameTeam
import net.spacetivity.blocko.utils.Constants
import net.spacetivity.blocko.utils.LocationUtils
import net.spacetivity.blocko.utils.ScoreboardUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.MagmaCube
import java.util.*
import kotlin.reflect.KClass

class GameFieldHighlightHandler {

    private val highlightEntities = mutableMapOf<UUID, Pair<ArenaId, HighlightMode>>()

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

        for (highlightMode in this.highlightModes.values) {
            ScoreboardUtils.registerScoreboardTeam(highlightMode.teamName, highlightMode.color)
        }
    }

    fun spawnOrUpdateHighlightEntity(arenaId: ArenaId, location: Location, highlightMode: HighlightMode) {
        val block = location.block
        val blockLocation = LocationUtils.centerLocation(block.location)

        var displayEntity = getHighlightEntity(arenaId, blockLocation)

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
        }

        val scoreboard = Bukkit.getScoreboardManager().mainScoreboard

        for (team in scoreboard.teams) {
            if (!team.hasEntity(displayEntity)) continue
            team.removeEntity(displayEntity)
        }

        scoreboard.getTeam(highlightMode.teamName)?.addEntity(displayEntity)

        this.highlightEntities[displayEntity.uniqueId] = Pair(arenaId, highlightMode)
    }

    fun removeHighlightEntities(arenaId: ArenaId, vararg highlightModeClasses: KClass<out HighlightMode>) {
        for (entityId in this.highlightEntities.keys.toList()) {
            val (currentArenaId, currentHighlightMode) = highlightEntities[entityId] ?: continue
            if (currentArenaId != arenaId) continue
            if (!highlightModeClasses.contains(currentHighlightMode::class)) continue

            this.highlightEntities.remove(entityId)

            val entity = Bukkit.getEntity(entityId) ?: continue

            for (team in Bukkit.getScoreboardManager().mainScoreboard.teams) {
                if (!team.hasEntity(entity)) continue
                team.removeEntity(entity)
            }

            Bukkit.getEntity(entityId)?.remove()
        }
    }

    fun getHighlightEntity(arenaId: ArenaId, location: Location): MagmaCube? {
        var displayEntity: MagmaCube? = null

        for ((entityId, arenaData) in this.highlightEntities) {
            if (arenaData.first != arenaId) continue

            val entity = Bukkit.getEntity(entityId) ?: continue
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