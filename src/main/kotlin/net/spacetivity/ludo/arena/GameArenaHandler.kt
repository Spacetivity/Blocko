package net.spacetivity.ludo.arena

import net.spacetivity.ludo.LudoGame
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class GameArenaHandler {

    val cachedArenas: MutableSet<GameArena> = mutableSetOf()

    init {
        transaction {
            for (resultRow: ResultRow in GameArenaDAO.selectAll().toMutableList()) {
                val id: String = resultRow[GameArenaDAO.id]
                val gameWorld: World = Bukkit.getWorld(resultRow[GameArenaDAO.worldName]) ?: continue

                val serializedLocation = resultRow[GameArenaDAO.playerLocation].split(":")
                val x: Double = serializedLocation[0].toDouble()
                val y: Double = serializedLocation[1].toDouble()
                val z: Double = serializedLocation[2].toDouble()
                val yaw: Float = serializedLocation[3].toFloat()
                val pitch: Float = serializedLocation[4].toFloat()

                val playerLocation = Location(gameWorld, x, y, z, yaw, pitch)
                val status: GameArenaOption.Status = GameArenaOption.Status.valueOf(resultRow[GameArenaDAO.status])

                cachedArenas.add(GameArena(id, gameWorld, playerLocation, status, GameArenaOption.Phase.IDLE))
            }
        }
    }

    fun updateArenaStatus(id: String, status: GameArenaOption.Status) {
        transaction {
            GameArenaDAO.update({ GameArenaDAO.id eq id }) { statement: UpdateStatement ->
                statement[GameArenaDAO.status] = status.name
            }
        }

        getArena(id)?.status = status
    }

    fun updateArenaPhase(id: String, phase: GameArenaOption.Phase) {
        getArena(id)?.phase = phase
    }

    fun createArena(worldName: String, location: Location): Boolean {
        val id: String = UUID.randomUUID().toString().split("-")[0]
        val serializedLocation = "${location.x}:${location.y}:${location.z}:${location.yaw}:${location.pitch}"
        val status: GameArenaOption.Status = GameArenaOption.Status.CONFIGURATING
        val phase: GameArenaOption.Phase = GameArenaOption.Phase.IDLE

        if (getArena(id) != null || this.cachedArenas.any { it.gameWorld.name.equals(worldName, true) }) return false

        transaction {
            GameArenaDAO.insert { statement: InsertStatement<Number> ->
                statement[GameArenaDAO.id] = id
                statement[GameArenaDAO.worldName] = worldName
                statement[playerLocation] = serializedLocation
                statement[maxPlayers] = 4
                statement[GameArenaDAO.status] = status.name
            }
        }

        this.cachedArenas.add(GameArena(id, Bukkit.getWorld(worldName)!!, location, status, phase))
        return true
    }

    fun deleteArena(id: String) {
        LudoGame.instance.gameTeamHandler.gameTeams.removeAll(id)
        LudoGame.instance.gameFieldHandler.deleteFields(id)
        LudoGame.instance.gameGarageFieldHandler.deleteGarageFields(id)
        LudoGame.instance.gameTeamHandler.deleteTeamSpawns(id)

        transaction {
            GameArenaDAO.deleteWhere { GameArenaDAO.id eq id }
        }


        this.cachedArenas.removeIf { it.id == id }
    }

    fun resetArenas() {
        this.cachedArenas.forEach(GameArena::reset)
    }

    fun getArena(id: String): GameArena? = this.cachedArenas.find { it.id == id }

}