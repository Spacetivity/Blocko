package net.spacetivity.ludo.arena.sign

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.transaction

class GameArenaSignHandler {

    private val cachedSignLocations: MutableList<Location> = mutableListOf()

    init {
        transaction {
            for (resultRow: ResultRow in GameArenaSignDAO.selectAll().toMutableList()) {
                val gameWorld: World = Bukkit.getWorld(resultRow[GameArenaSignDAO.worldName]) ?: continue
                val x: Double = resultRow[GameArenaSignDAO.x]
                val y: Double = resultRow[GameArenaSignDAO.y]
                val z: Double = resultRow[GameArenaSignDAO.z]

                cachedSignLocations.add(Location(gameWorld, x, y, z))
            }
        }
    }

    fun existsLocation(location: Location): Boolean {
        return this.cachedSignLocations.any { it.x == location.x && it.y == location.y && it.z == location.z }
    }

    fun createSignLocation(location: Location) {
        transaction {
            GameArenaSignDAO.insert { statement: InsertStatement<Number> ->
                statement[worldName] = location.world.name
                statement[x] = location.x
                statement[y] = location.y
                statement[z] = location.z
            }
        }

        this.cachedSignLocations.add(location)
    }

    fun deleteArenaSign(location: Location) {
        transaction {
            GameArenaSignDAO.deleteIgnoreWhere {
                (worldName eq location.world.name) and (x eq location.x) and (y eq location.y) and (z eq location.z)
            }
        }

        this.cachedSignLocations.removeIf { it.world.name == location.world.name && it.x == location.x && it.y == location.y && it.z == location.z }
    }

    fun loadArenaSigns() {
        for (index: Int in this.cachedSignLocations.indices) {
            val location: Location = this.cachedSignLocations[index]
            val gameArena: GameArena? = LudoGame.instance.gameArenaHandler.cachedArenas.getOrNull(index)
            LudoGame.instance.gameArenaHandler.loadJoinSign(location, gameArena)
        }
    }

}