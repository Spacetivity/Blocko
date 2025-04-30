package net.spacetivity.blocko.field.highlighting

import net.spacetivity.blocko.arena.id.ArenaId
import net.spacetivity.blocko.field.highlighting.scoreboard.HighlightMode
import net.spacetivity.blocko.field.highlighting.scoreboard.impl.*
import net.spacetivity.blocko.utils.Constants
import net.spacetivity.blocko.utils.LocationUtils
import net.spacetivity.blocko.utils.ScoreboardUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.MagmaCube
import java.util.*
import kotlin.reflect.KClass

class GameFieldHighlightHandler {

    private val highlightEntities = mutableMapOf<UUID, Pair<ArenaId, HighlightMode>>()

    val highlightModes = mutableMapOf(
        Pair(GameFieldHighlightMode::class, GameFieldHighlightMode()),
        Pair(TurningPointHighlightMode::class, TurningPointHighlightMode()),
        Pair(GarageFieldHighlightMode::class, GarageFieldHighlightMode()),
        Pair(GameEntityGoalFieldHighlightMode::class, GameEntityGoalFieldHighlightMode()),
    )

    init {
        for (gameTeam in Constants.GAME_TEAMS) {
            this.highlightModes.put(TeamSpawnHighlightMode::class, TeamSpawnHighlightMode(gameTeam))
            this.highlightModes.put(TeamPathHighlightMode::class, TeamPathHighlightMode(gameTeam))
        }

        for (highlightMode in this.highlightModes.values) {
            ScoreboardUtils.registerScoreboardTeam(highlightMode.teamName, highlightMode.color)
        }
    }

    fun spawnOrUpdateHighlightEntity(arenaId: ArenaId, location: Location, highlightModeClass: KClass<out HighlightMode>) {
        val block = location.block
        val blockLocation =  LocationUtils.centerLocation(block.location)

        var displayEntity = getHighlightEntity(arenaId, blockLocation)

        if (displayEntity == null) {
            displayEntity = blockLocation.world.spawnEntity(blockLocation, EntityType.MAGMA_CUBE) as MagmaCube
            displayEntity.size = 2
        }

        displayEntity.isGlowing = true
        displayEntity.isSilent = true
        displayEntity.isInvulnerable = true
        displayEntity.isCollidable = false
        displayEntity.isAggressive = false
        displayEntity.isAware = false
        displayEntity.isInvisible = true
        displayEntity.setGravity(false)
        displayEntity.setAI(false)

        val scoreboard = Bukkit.getScoreboardManager().mainScoreboard

        for (team in scoreboard.teams) {
            if (!team.hasEntity(displayEntity)) continue
            team.removeEntity(displayEntity)
        }

        val highlightMode = getHighlightMode(highlightModeClass) ?: return

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
    private fun <T : HighlightMode> getHighlightMode(clazz: KClass<T>): T? {
        return this.highlightModes[clazz] as? T
    }

}