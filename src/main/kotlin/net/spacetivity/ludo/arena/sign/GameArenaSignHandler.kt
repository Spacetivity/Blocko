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

    private val cachedArenaSigns: MutableList<GameArenaSign> = mutableListOf()

    init {
        transaction {
            val resultRows: MutableList<ResultRow> = GameArenaSignDAO.selectAll().toMutableList()

            for (index: Int in resultRows.indices) {
                val resultRow: ResultRow = resultRows[index]
                val gameWorld: World = Bukkit.getWorld(resultRow[GameArenaSignDAO.worldName]) ?: continue
                val x: Double = resultRow[GameArenaSignDAO.x]
                val y: Double = resultRow[GameArenaSignDAO.y]
                val z: Double = resultRow[GameArenaSignDAO.z]

                val gameArena: GameArena? = LudoGame.instance.gameArenaHandler.cachedArenas.getOrNull(index)
                cachedArenaSigns.add(GameArenaSign(Location(gameWorld, x, y, z), gameArena?.id))
            }
        }
    }

    fun existsLocation(location: Location): Boolean {
        return this.cachedArenaSigns.any { it.location.world.name == location.world.name && it.location.x == location.x && it.location.y == location.y && it.location.z == location.z }
    }

    fun getSign(location: Location): GameArenaSign? {
        return this.cachedArenaSigns.find{ it.location.world.name == location.world.name && it.location.x == location.x && it.location.y == location.y && it.location.z == location.z }
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

        this.cachedArenaSigns.add(GameArenaSign(location, null))
        recalculateSignData()
    }

    fun deleteArenaSign(location: Location) {
        transaction {
            GameArenaSignDAO.deleteIgnoreWhere {
                (worldName eq location.world.name) and (x eq location.x) and (y eq location.y) and (z eq location.z)
            }
        }

        this.cachedArenaSigns.removeIf { it.location.world.name == location.world.name && it.location.x == location.x && it.location.y == location.y && it.location.z == location.z }
        recalculateSignData()
    }

    fun updateArenaSign(gameArena: GameArena) {
        val arenaSign: GameArenaSign = this.cachedArenaSigns.find { it.arenaId == gameArena.id } ?: return
        LudoGame.instance.gameArenaHandler.loadJoinSign(arenaSign.location, gameArena)
    }

    fun loadArenaSigns() {
        for (arenaSign: GameArenaSign in this.cachedArenaSigns) {
            val arenaId: String? = arenaSign.arenaId
            val gameArena: GameArena? = if (arenaId == null) null else LudoGame.instance.gameArenaHandler.getArena(arenaId)
            LudoGame.instance.gameArenaHandler.loadJoinSign(arenaSign.location, gameArena)
        }
    }

    private fun recalculateSignData() {
        for (index: Int in this.cachedArenaSigns.indices) {
            cachedArenaSigns[index].arenaId = LudoGame.instance.gameArenaHandler.cachedArenas.getOrNull(index)?.id
        }
    }

}