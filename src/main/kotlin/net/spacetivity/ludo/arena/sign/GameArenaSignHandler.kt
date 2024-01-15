package net.spacetivity.ludo.arena.sign

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
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
        if (existsLocation(location)) return

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

    fun loadArenaSigns() { //TODO: Maybe add an 'id' field to the SIGN DAO to sort the signs correctly!
        for (index: Int in this.cachedSignLocations.indices) {
            val location: Location = this.cachedSignLocations[index]
            val gameArena: GameArena? = LudoGame.instance.gameArenaHandler.cachedArenas.getOrNull(index)
            LudoGame.instance.gameArenaHandler.loadJoinSign(location, gameArena)

        }
    }

}