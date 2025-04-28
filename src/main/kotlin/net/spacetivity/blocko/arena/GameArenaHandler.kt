package net.spacetivity.blocko.arena

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.phase.impl.EndingPhase
import net.spacetivity.blocko.phase.impl.IdlePhase
import net.spacetivity.blocko.phase.impl.IngamePhase
import net.spacetivity.blocko.player.GamePlayer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.block.Sign
import org.bukkit.block.sign.Side
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.*

class GameArenaHandler {

    private val gamePhaseHandler = BlockoGame.instance.gamePhaseHandler
    val cachedArenas = mutableListOf<GameArena>()

    init {
        transaction {
            for (resultRow in GameArenaDAO.selectAll().toMutableList()) {
                val arenaId = resultRow[GameArenaDAO.id]

                val worldName = resultRow[GameArenaDAO.worldName]
                var gameWorld: World? = null

                if (Bukkit.getWorld(worldName) == null)
                    gameWorld = WorldCreator(worldName).createWorld()

                if (gameWorld == null) {
                    println("Cannot load game world $worldName!")
                    continue
                }

                val serializedLocation = resultRow[GameArenaDAO.playerLocation].split(":")
                val x = serializedLocation[0].toDouble()
                val y = serializedLocation[1].toDouble()
                val z = serializedLocation[2].toDouble()
                val yaw = serializedLocation[3].toFloat()
                val pitch = serializedLocation[4].toFloat()

                val playerLocation = Location(gameWorld, x, y, z, yaw, pitch)
                val status = GameArenaStatus.valueOf(resultRow[GameArenaDAO.status])

                val idlePhase = IdlePhase(arenaId)
                gamePhaseHandler.cachedGamePhases.put(arenaId, idlePhase)
                gamePhaseHandler.cachedGamePhases.put(arenaId, IngamePhase(arenaId))
                gamePhaseHandler.cachedGamePhases.put(arenaId, EndingPhase(arenaId))

                cachedArenas.add(GameArena(arenaId, gameWorld, status, idlePhase, playerLocation.y, playerLocation))
            }
        }
    }

    fun updateArenaStatus(id: String, status: GameArenaStatus) {
        transaction {
            GameArenaDAO.update({ GameArenaDAO.id eq id }) { statement: UpdateStatement ->
                statement[GameArenaDAO.status] = status.name
            }
        }

        getArena(id)?.status = status
    }

    fun createArena(worldName: String, location: Location): Boolean {
        val id = UUID.randomUUID().toString().split("-")[0]
        val serializedLocation = "${location.x}:${location.y}:${location.z}:${location.yaw}:${location.pitch}"
        val status = GameArenaStatus.CONFIGURATING

        if (getArena(id) != null || (BlockoGame.instance.gameArenaHandler.cachedArenas.size >= BlockoGame.instance.globalConfigFile.gameArenaMaxParallelAmount)) return false

        transaction {
            GameArenaDAO.insert { statement ->
                statement[GameArenaDAO.id] = id
                statement[GameArenaDAO.worldName] = worldName
                statement[playerLocation] = serializedLocation
                statement[maxPlayers] = 4
                statement[GameArenaDAO.status] = status.name
            }
        }

        val idlePhase = IdlePhase(id)
        gamePhaseHandler.cachedGamePhases.put(id, idlePhase)
        gamePhaseHandler.cachedGamePhases.put(id, IngamePhase(id))
        gamePhaseHandler.cachedGamePhases.put(id, EndingPhase(id))

        this.cachedArenas.add(GameArena(id, Bukkit.getWorld(worldName)!!, status, idlePhase, location.y, location))
        return true
    }

    fun deleteArena(id: String) {
        BlockoGame.instance.gameFieldHandler.deleteFields(id)
        BlockoGame.instance.gameTeamHandler.deleteTeamSpawns(id)
        BlockoGame.instance.gamePhaseHandler.deletePhases(id)
        BlockoGame.instance.gameTeamHandler.gameTeams.removeAll(id)

        transaction {
            GameArenaDAO.deleteWhere { GameArenaDAO.id eq id }
        }

        this.cachedArenas.removeIf { it.id == id }
    }

    fun resetArenas(shutdown: Boolean) {
        this.cachedArenas.forEach {
            it.reset(shutdown)
        }
    }

    fun getArena(id: String): GameArena? {
        return this.cachedArenas.find { it.id == id }
    }

    fun getArenaOfPlayer(uuid: UUID): GameArena? {
        return this.cachedArenas.find { it.currentPlayers.any { gamePlayer: GamePlayer -> gamePlayer.uuid == uuid } } ?: getArenaOfSpectator(uuid)
    }

    fun getArenaOfSpectator(uuid: UUID): GameArena? {
        return this.cachedArenas.find { it.spectatorPlayers.contains(uuid) }
    }

    //TODO: make the sign layout configurable
    fun loadJoinSign(location: Location, gameArena: GameArena?) {
        val block = location.block
        if (!block.type.name.contains("WALL_SIGN", true)) return

        val sign = block.state as Sign
        val signSide = sign.getSide(Side.FRONT)

        signSide.line(0, Component.text("BLOCKO", NamedTextColor.BLUE, TextDecoration.BOLD))

        if (gameArena == null) {
            signSide.line(1, Component.text("Searching", NamedTextColor.GRAY))
            signSide.line(2, Component.text("for arena...", NamedTextColor.GRAY))
        } else {
            val arenaStatus = gameArena.status
            val arenaPhase = gameArena.phase

            val statusLine = when (arenaStatus) {
                GameArenaStatus.READY -> when (arenaPhase) {
                    is IdlePhase -> Component.text("${gameArena.currentPlayers.size}/${gameArena.teamOptions.playerCount}", NamedTextColor.YELLOW)
                    is IngamePhase -> Component.text("Ingame...", NamedTextColor.RED)
                    is EndingPhase -> Component.text("Ending...", NamedTextColor.RED)
                    else -> Component.text("Phase 404", NamedTextColor.RED)
                }

                GameArenaStatus.CONFIGURATING -> Component.text("Configuration...", NamedTextColor.RED)
                GameArenaStatus.RESETTING -> Component.text("Resetting...", NamedTextColor.RED)
            }

            signSide.line(1, Component.text(gameArena.teamOptions.getDisplayString(), NamedTextColor.AQUA))
            signSide.line(2, statusLine)

            if (arenaStatus == GameArenaStatus.READY && arenaPhase.isIdle()) {
                signSide.line(3, Component.text("[JOIN]", NamedTextColor.GREEN))
            } else if (arenaStatus == GameArenaStatus.READY && arenaPhase.isIngame()) {
                signSide.line(3, Component.text("[SPECTATE]", NamedTextColor.GOLD))
            } else {
                signSide.line(3, Component.text(" "))
            }
        }

        sign.update()
    }

}