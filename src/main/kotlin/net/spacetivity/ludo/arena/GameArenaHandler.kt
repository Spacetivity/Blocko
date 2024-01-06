package net.spacetivity.ludo.arena

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class GameArenaHandler {

    val cachedArenas: MutableSet<GameArena> = mutableSetOf()

    fun loadArena(id: String) {
        transaction {
            GameArenaDAO.select { GameArenaDAO.id eq id }.map { resultRow: ResultRow ->
                val gameWorld: World = Bukkit.getWorld(resultRow[GameArenaDAO.worldName]) ?: return@map

                val serializedLocation = resultRow[GameArenaDAO.playerLocation].split(":")
                val x: Double = serializedLocation[0].toDouble()
                val y: Double = serializedLocation[1].toDouble()
                val z: Double = serializedLocation[2].toDouble()
                val yaw: Float = serializedLocation[3].toFloat()
                val pitch: Float = serializedLocation[4].toFloat()

                val playerLocation = Location(gameWorld, x, y, z, yaw, pitch)

                val gameArena = GameArena(gameWorld, playerLocation)

                cachedArenas.add(gameArena)
            }
        }
    }

    fun createArena(worldName: String, location: Location) {
        val id: String = UUID.randomUUID().toString().split("-")[0]
        val serializedLocation = "${location.x}:${location.y}:${location.z}:${location.yaw}:${location.pitch}"

        transaction {
            GameArenaDAO.insert { statement: InsertStatement<Number> ->
                statement[GameArenaDAO.id] = id
                statement[GameArenaDAO.worldName] = worldName
                statement[playerLocation] = serializedLocation
                statement[maxPlayers] = 4
            }
        }

    }

    fun resetArenas() {
        this.cachedArenas.forEach(GameArena::resetArena)
    }

    fun getArena(id: String): GameArena? = this.cachedArenas.find { it.id == id }

    object GameArenaDAO : Table("game_arenas") {
        val id: Column<String> = varchar("id", 10).uniqueIndex()
        val worldName: Column<String> = varchar("id", 30)
        val playerLocation: Column<String> = varchar("playerLocation", 30)
        val maxPlayers: Column<Int> = integer("maxPlayers")
    }

}