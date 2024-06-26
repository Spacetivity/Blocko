package net.spacetivity.blocko.team

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.utils.MetadataUtils
import org.bukkit.entity.LivingEntity
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class GameTeamHandler {

    val gameTeams: Multimap<String, GameTeam> = ArrayListMultimap.create()

    init {
        for (gameArena: GameArena in BlockoGame.instance.gameArenaHandler.cachedArenas) {
            addTeam(gameArena.id, GameTeam("red", NamedTextColor.RED, 0))
            addTeam(gameArena.id, GameTeam("blue", NamedTextColor.BLUE, 1))
            addTeam(gameArena.id, GameTeam("yellow", NamedTextColor.YELLOW, 2))
            addTeam(gameArena.id, GameTeam("green", NamedTextColor.GREEN, 3))
        }

        transaction {
            for (resultRow: ResultRow in GameTeamLocationDAO.selectAll().toMutableList()) {
                val arenaId: String = resultRow[GameTeamLocationDAO.arenaId]
                val teamName: String = resultRow[GameTeamLocationDAO.teamName]
                val worldName: String = resultRow[GameTeamLocationDAO.worldName]
                val x: Double = resultRow[GameTeamLocationDAO.x]
                val y: Double = resultRow[GameTeamLocationDAO.y]
                val z: Double = resultRow[GameTeamLocationDAO.z]
                val yaw: Float = resultRow[GameTeamLocationDAO.yaw]
                val pitch: Float = resultRow[GameTeamLocationDAO.pitch]

                val gameTeam: GameTeam = getTeam(arenaId, teamName) ?: continue
                gameTeam.teamLocations.add(GameTeamLocation(arenaId, teamName, worldName, x, y, z, yaw, pitch, false))
            }
        }
    }

    fun initTeamSpawns(gameTeamLocations: MutableList<GameTeamLocation>) {
        transaction {
            for (gameTeamLocation: GameTeamLocation in gameTeamLocations) {
                GameTeamLocationDAO.insert { statement: InsertStatement<Number> ->
                    statement[arenaId] = gameTeamLocation.arenaId
                    statement[teamName] = gameTeamLocation.teamName
                    statement[worldName] = gameTeamLocation.worldName
                    statement[x] = gameTeamLocation.x
                    statement[y] = gameTeamLocation.y
                    statement[z] = gameTeamLocation.z
                    statement[yaw] = gameTeamLocation.yaw
                    statement[pitch] = gameTeamLocation.pitch
                }

                val gameTeam: GameTeam = getTeam(gameTeamLocation.arenaId, gameTeamLocation.teamName) ?: continue
                gameTeam.teamLocations.add(gameTeamLocation)
            }
        }
    }

    fun deleteTeamSpawns(arenaId: String) {
        transaction {
            GameTeamLocationDAO.deleteWhere { GameTeamLocationDAO.arenaId eq arenaId }
        }
    }

    fun addTeam(arenaId: String, gameTeam: GameTeam) {
        this.gameTeams.put(arenaId, gameTeam)
    }

    fun getTeamOfEntity(arenaId: String, entity: LivingEntity): GameTeam? {
        val teamName: String = MetadataUtils.get<String>(entity, "teamName") ?: return null
        return getTeam(arenaId, teamName)
    }

    fun getTeamOfPlayer(arenaId: String, uuid: UUID): GameTeam? {
        return this.gameTeams.get(arenaId).firstOrNull() { it.teamMembers.contains(uuid) }
    }

    fun getTeam(arenaId: String, name: String): GameTeam? {
        return this.gameTeams.get(arenaId).find { it.name.equals(name, true) }
    }

    fun getLocationsOfAllTeams(arenaId: String): Collection<GameTeamLocation> {
        val locations: MutableList<GameTeamLocation> = mutableListOf()
        for (gameTeam in this.gameTeams[arenaId]) locations.addAll(gameTeam.teamLocations)
        return locations
    }

    fun getLocationOfTeam(arenaId: String, teamName: String, x: Double, y: Double, z: Double): GameTeamLocation? {
        val teamLocations: List<GameTeamLocation> = getLocationsOfAllTeams(arenaId).filter { it.teamName == teamName }

        var result: GameTeamLocation? = null

        for (teamLocation: GameTeamLocation in teamLocations) {
            val worldPosition = teamLocation.getWorldPosition()
            if (!(worldPosition.x == x && worldPosition.y == y && worldPosition.z == z)) continue
            result = teamLocation
        }

        return result
    }

}