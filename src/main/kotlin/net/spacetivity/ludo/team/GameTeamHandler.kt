package net.spacetivity.ludo.team

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.utils.MetadataUtils
import org.bukkit.entity.LivingEntity
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class GameTeamHandler {

    val gameTeams: Multimap<String, GameTeam> = ArrayListMultimap.create()

    init {
        for (gameArena: GameArena in LudoGame.instance.gameArenaHandler.cachedArenas) {
            addTeam(gameArena.id, GameTeam("red", NamedTextColor.RED))
            addTeam(gameArena.id, GameTeam("green", NamedTextColor.GREEN))
            addTeam(gameArena.id, GameTeam("blue", NamedTextColor.BLUE))
            addTeam(gameArena.id, GameTeam("yellow", NamedTextColor.YELLOW))
        }

        transaction {
            for (resultRow: ResultRow in GameTeamSpawnDAO.selectAll().toMutableList()) {
                val arenaId: String = resultRow[GameTeamSpawnDAO.arenaId]
                val teamName: String = resultRow[GameTeamSpawnDAO.teamName]
                val worldName: String = resultRow[GameTeamSpawnDAO.worldName]
                val x: Double = resultRow[GameTeamSpawnDAO.x]
                val y: Double = resultRow[GameTeamSpawnDAO.y]
                val z: Double = resultRow[GameTeamSpawnDAO.z]
                val yaw: Float = resultRow[GameTeamSpawnDAO.yaw]
                val pitch: Float = resultRow[GameTeamSpawnDAO.pitch]

                val gameTeam: GameTeam = getTeam(arenaId, teamName) ?: continue
                gameTeam.teamSpawnLocations.add(GameTeamSpawn(arenaId, teamName, worldName, x, y, z, yaw, pitch, false))
            }
        }
    }

    fun initTeamSpawns(gameTeamSpawns: MutableList<GameTeamSpawn>) {
        transaction {
            for (gameTeamSpawn: GameTeamSpawn in gameTeamSpawns) {
                GameTeamSpawnDAO.insert { statement: InsertStatement<Number> ->
                    statement[arenaId] = gameTeamSpawn.arenaId
                    statement[teamName] = gameTeamSpawn.teamName
                    statement[worldName] = gameTeamSpawn.worldName
                    statement[x] = gameTeamSpawn.x
                    statement[y] = gameTeamSpawn.y
                    statement[z] = gameTeamSpawn.z
                    statement[yaw] = gameTeamSpawn.yaw
                    statement[pitch] = gameTeamSpawn.pitch
                }

                val gameTeam: GameTeam = getTeam(gameTeamSpawn.arenaId, gameTeamSpawn.teamName) ?: continue
                gameTeam.teamSpawnLocations.add(gameTeamSpawn)
            }
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
        return this.gameTeams.get(arenaId).first { it.teamMembers.contains(uuid) }
    }

    fun getTeam(arenaId: String, name: String): GameTeam? {
        return this.gameTeams.get(arenaId).find { it.name.equals(name, true) }
    }

}