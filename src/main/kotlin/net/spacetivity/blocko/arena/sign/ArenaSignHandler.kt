package net.spacetivity.blocko.arena.sign

import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.arena.Arena
import net.spacetivity.blocko.arena.id.ArenaId
import org.bukkit.Bukkit
import org.bukkit.Location
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteIgnoreWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class ArenaSignHandler {

    private val cachedArenaSigns = mutableListOf<ArenaSign>()

    init {
        transaction {
            val resultRows = ArenaSignDAO.selectAll().toMutableList()

            for (index in resultRows.indices) {
                val resultRow = resultRows[index]
                val gameWorld = Bukkit.getWorld(resultRow[ArenaSignDAO.worldName]) ?: continue
                val x: Double = resultRow[ArenaSignDAO.x]
                val y: Double = resultRow[ArenaSignDAO.y]
                val z: Double = resultRow[ArenaSignDAO.z]

                val gameArena = Blocko.instance.arenaHandler.cachedArenas.getOrNull(index)
                cachedArenaSigns.add(ArenaSign(Location(gameWorld, x, y, z), gameArena?.id))
            }
        }
    }

    fun existsLocation(location: Location): Boolean {
        return this.cachedArenaSigns.any { it.location.world.name == location.world.name && it.location.x == location.x && it.location.y == location.y && it.location.z == location.z }
    }

    fun getSign(arenaId: ArenaId): ArenaSign? {
        return this.cachedArenaSigns.find { it.arenaId == arenaId }
    }

    fun getSign(location: Location): ArenaSign? {
        return this.cachedArenaSigns.find{ it.location.world.name == location.world.name && it.location.x == location.x && it.location.y == location.y && it.location.z == location.z }
    }

    fun createSignLocation(location: Location) {
        transaction {
            ArenaSignDAO.insert { statement ->
                statement[worldName] = location.world.name
                statement[x] = location.x
                statement[y] = location.y
                statement[z] = location.z
            }
        }

        this.cachedArenaSigns.add(ArenaSign(location, null))
        recalculateSignData()
    }

    fun deleteArenaSign(location: Location) {
        transaction {
            ArenaSignDAO.deleteIgnoreWhere {
                (worldName eq location.world.name) and (x eq location.x) and (y eq location.y) and (z eq location.z)
            }
        }

        this.cachedArenaSigns.removeIf { it.location.world.name == location.world.name && it.location.x == location.x && it.location.y == location.y && it.location.z == location.z }
        recalculateSignData()
    }

    fun updateArenaSign(arena: Arena) {
        val arenaSign = this.cachedArenaSigns.find { it.arenaId == arena.id } ?: return
        Blocko.instance.arenaHandler.loadJoinSign(arenaSign.location, arena)
    }

    fun loadArenaSigns() {
        for (arenaSign in this.cachedArenaSigns) {
            val arenaId = arenaSign.arenaId
            val gameArena = if (arenaId == null) null else Blocko.instance.arenaHandler.getArena(arenaId)
            Blocko.instance.arenaHandler.loadJoinSign(arenaSign.location, gameArena)
        }
    }

    private fun recalculateSignData() {
        for (index in this.cachedArenaSigns.indices) {
            cachedArenaSigns[index].arenaId = Blocko.instance.arenaHandler.cachedArenas.getOrNull(index)?.id
        }
    }

}