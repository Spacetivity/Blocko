package net.spacetivity.blocko.lobby

import org.bukkit.Location
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class LobbySpawnHandler {

    var lobbySpawn: LobbySpawn? = null

    init {
        transaction {
            val resultRow: ResultRow = LobbySpawnDAO.selectAll().firstOrNull() ?: return@transaction
            lobbySpawn = LobbySpawn(
                resultRow[LobbySpawnDAO.worldName],
                resultRow[LobbySpawnDAO.x],
                resultRow[LobbySpawnDAO.y],
                resultRow[LobbySpawnDAO.z],
                resultRow[LobbySpawnDAO.yaw],
                resultRow[LobbySpawnDAO.pitch]
            )
        }
    }

    fun setLobbySpawn(location: Location) {
        if (this.lobbySpawn != null) {
            updateLobbySpawn(location)
            return
        }

        val lobbySpawn = LobbySpawn(location.world.name, location.x, location.y, location.z, location.yaw, location.pitch)
        this.lobbySpawn = lobbySpawn

        transaction {
            LobbySpawnDAO.insert { statement: InsertStatement<Number> ->
                statement[worldName] = lobbySpawn.worldName
                statement[x] = lobbySpawn.x
                statement[y] = lobbySpawn.y
                statement[z] = lobbySpawn.z
                statement[yaw] = lobbySpawn.yaw
                statement[pitch] = lobbySpawn.pitch
            }
        }
    }

    fun updateLobbySpawn(newLocation: Location) {
        transaction {
            LobbySpawnDAO.update({ LobbySpawnDAO.x eq newLocation.x }) { statement: UpdateStatement ->
                statement[worldName] = newLocation.world.name
                statement[x] = newLocation.x
                statement[y] = newLocation.y
                statement[z] = newLocation.z
                statement[yaw] = newLocation.yaw
                statement[pitch] = newLocation.pitch
            }
        }

        this.lobbySpawn = LobbySpawn(newLocation.world.name, newLocation.x, newLocation.y, newLocation.z, newLocation.yaw, newLocation.pitch)
    }

}