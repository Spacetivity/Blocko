package net.spacetivity.blocko.team

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.arena.id.ArenaId
import net.spacetivity.blocko.utils.Constants
import net.spacetivity.blocko.utils.Constants.TEAM_NAME_KEY
import net.spacetivity.blocko.utils.MetadataUtils
import org.bukkit.entity.LivingEntity
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class GameTeamHandler {

    val gameTeams: Multimap<ArenaId, GameTeam> = ArrayListMultimap.create()

    init {
        for (arena in Blocko.instance.arenaHandler.cachedArenas) {
            this.gameTeams.putAll(arena.id, Constants.GAME_TEAMS)
        }

        transaction {
            for (resultRow in GameTeamLocationDAO.selectAll().toMutableList()) {
                val arenaId = resultRow[GameTeamLocationDAO.arenaId]
                val teamName = resultRow[GameTeamLocationDAO.teamName]
                val worldName = resultRow[GameTeamLocationDAO.worldName]
                val x = resultRow[GameTeamLocationDAO.x]
                val y = resultRow[GameTeamLocationDAO.y]
                val z = resultRow[GameTeamLocationDAO.z]
                val yaw = resultRow[GameTeamLocationDAO.yaw]
                val pitch = resultRow[GameTeamLocationDAO.pitch]

                val gameTeam = getTeam(arenaId, teamName) ?: continue
                gameTeam.teamLocations.add(GameTeamLocation(arenaId, teamName, worldName, x, y, z, yaw, pitch, false))
            }
        }
    }

    fun initTeamSpawns(gameTeamLocations: MutableList<GameTeamLocation>) {
        transaction {
            for (gameTeamLocation: GameTeamLocation in gameTeamLocations) {
                GameTeamLocationDAO.insert { statement ->
                    statement[arenaId] = gameTeamLocation.arenaId
                    statement[teamName] = gameTeamLocation.teamName
                    statement[worldName] = gameTeamLocation.worldName
                    statement[x] = gameTeamLocation.x
                    statement[y] = gameTeamLocation.y
                    statement[z] = gameTeamLocation.z
                    statement[yaw] = gameTeamLocation.yaw
                    statement[pitch] = gameTeamLocation.pitch
                }

                val gameTeam = getTeam(gameTeamLocation.arenaId, gameTeamLocation.teamName) ?: continue
                gameTeam.teamLocations.add(gameTeamLocation)
            }
        }
    }

    fun deleteTeamSpawns(arenaId: ArenaId) {
        transaction {
            GameTeamLocationDAO.deleteWhere { GameTeamLocationDAO.arenaId eq arenaId }
        }
    }

    fun addTeam(arenaId: ArenaId, gameTeam: GameTeam) {
        this.gameTeams.put(arenaId, gameTeam)
    }

    fun getTeamOfEntity(arenaId: ArenaId, entity: LivingEntity): GameTeam? {
        val teamName = MetadataUtils.get<String>(entity, TEAM_NAME_KEY) ?: return null
        return getTeam(arenaId, teamName)
    }

    fun getTeamOfPlayer(arenaId: ArenaId, uuid: UUID): GameTeam? {
        return this.gameTeams.get(arenaId).firstOrNull { it.teamMembers.contains(uuid) }
    }

    fun getTeam(arenaId: ArenaId, name: String): GameTeam? {
        return this.gameTeams.get(arenaId).find { it.name.equals(name, true) }
    }

    fun getLocationsOfAllTeams(arenaId: ArenaId): Collection<GameTeamLocation> {
        val locations: MutableList<GameTeamLocation> = mutableListOf()
        for (gameTeam in this.gameTeams[arenaId]) locations.addAll(gameTeam.teamLocations)
        return locations
    }

    fun getLocationOfTeam(arenaId: ArenaId, teamName: String, x: Double, y: Double, z: Double): GameTeamLocation? {
        var result: GameTeamLocation? = null
        val teamLocations = getLocationsOfAllTeams(arenaId).filter { it.teamName == teamName }

        for (teamLocation: GameTeamLocation in teamLocations) {
            val worldPosition = teamLocation.getWorldPosition()
            if (!(worldPosition.x == x && worldPosition.y == y && worldPosition.z == z)) continue
            result = teamLocation
        }

        return result
    }

}