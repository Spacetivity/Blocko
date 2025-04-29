package net.spacetivity.blocko.field.highlighting

import net.spacetivity.blocko.arena.id.ArenaId
import net.spacetivity.blocko.field.highlighting.scoreboard.HighlightMode
import net.spacetivity.blocko.field.highlighting.scoreboard.impl.*
import net.spacetivity.blocko.utils.Constants
import net.spacetivity.blocko.utils.ScoreboardUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.EntityType
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f
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
            highlightModes.put(TeamSpawnHighlightMode::class, TeamSpawnHighlightMode(gameTeam))
            highlightModes.put(TeamPathHighlightMode::class, TeamPathHighlightMode(gameTeam))
        }

        for (highlightMode in this.highlightModes.values) {
            ScoreboardUtils.registerScoreboardTeam(highlightMode.teamName, highlightMode.color)
        }
    }

    fun spawnOrUpdateHighlightEntity(arenaId: ArenaId, location: Location, highlightModeClass: KClass<out HighlightMode>) {
        val block = location.block
        val centerLocation = block.location.toCenterLocation()

        var displayEntity = getHighlightEntity(arenaId, location)

        if (displayEntity == null) {
            val scale = 1.0f
            displayEntity = location.world.spawnEntity(centerLocation, EntityType.BLOCK_DISPLAY) as BlockDisplay
            displayEntity.transformation = Transformation(Vector3f(0f, 0f, 0f), Quaternionf(), Vector3f(scale, scale, scale), Quaternionf())
        }

        displayEntity.isGlowing = false

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
        val tempMap = this.highlightEntities
        for ((entityId, arenaData) in tempMap) {
            if (arenaData.first != arenaId) continue
            if (!highlightModeClasses.contains(arenaData.second::class)) continue

            this.highlightEntities.remove(entityId)

            val entity = Bukkit.getEntity(entityId) ?: continue

            for (team in Bukkit.getScoreboardManager().mainScoreboard.teams) {
                if (!team.hasEntity(entity)) continue
                team.removeEntity(entity)
            }

            Bukkit.getEntity(entityId)?.remove()
        }
    }

    fun getHighlightEntity(arenaId: ArenaId, location: Location): BlockDisplay? {
        var displayEntity: BlockDisplay? = null

        for ((entityId, arenaData) in this.highlightEntities) {
            if (arenaData.first != arenaId) continue

            val entity = Bukkit.getEntity(entityId) ?: continue
            if (entity !is BlockDisplay) continue
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