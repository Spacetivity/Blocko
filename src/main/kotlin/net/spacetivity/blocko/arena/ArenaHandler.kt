package net.spacetivity.blocko.arena

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.arena.id.ArenaId
import net.spacetivity.blocko.phase.impl.EndingPhase
import net.spacetivity.blocko.phase.impl.IdlePhase
import net.spacetivity.blocko.phase.impl.IngamePhase
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.utils.Constants
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
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.*

class ArenaHandler {

    private val gamePhaseHandler = Blocko.instance.gamePhaseHandler
    val cachedArenaIds = mutableListOf<ArenaId>()
    val cachedArenas = mutableListOf<Arena>()

    // Cache für aktive Ingame-Arenen zur Performance-Optimierung
    val activeIngameArenas = mutableSetOf<Arena>()

    init {
        transaction {
            for (resultRow in ArenaDAO.selectAll().toMutableList()) {
                val arenaId = resultRow[ArenaDAO.id]

                val worldName = resultRow[ArenaDAO.worldName]
                var gameWorld: World? = null

                if (Bukkit.getWorld(worldName) == null)
                    gameWorld = WorldCreator(worldName).createWorld()

                if (gameWorld == null) {
                    Bukkit.getConsoleSender()
                        .sendMessage(Component.text("Cannot load game world $worldName!", NamedTextColor.RED))
                    continue
                }

                val serializedLocation = resultRow[ArenaDAO.playerLocation].split(":")
                val x = serializedLocation[0].toDouble()
                val y = serializedLocation[1].toDouble()
                val z = serializedLocation[2].toDouble()
                val yaw = serializedLocation[3].toFloat()
                val pitch = serializedLocation[4].toFloat()

                val playerLocation = Location(gameWorld, x, y, z, yaw, pitch)
                val status = ArenaStatus.valueOf(resultRow[ArenaDAO.status])

                val idlePhase = IdlePhase(arenaId)
                gamePhaseHandler.cachedGamePhases.put(arenaId, idlePhase)
                gamePhaseHandler.cachedGamePhases.put(arenaId, IngamePhase(arenaId))
                gamePhaseHandler.cachedGamePhases.put(arenaId, EndingPhase(arenaId))

                cachedArenaIds.add(arenaId)
                cachedArenas.add(Arena(arenaId, gameWorld, status, idlePhase, playerLocation.y, playerLocation))
            }
        }
    }

    fun updateArenaStatus(id: ArenaId, status: ArenaStatus) {
        transaction {
            ArenaDAO.update({ ArenaDAO.id eq id }) { statement ->
                statement[ArenaDAO.status] = status.name
            }
        }

        getArena(id)?.status = status
    }

    fun createArena(worldName: String, location: Location): Boolean {
        val id = ArenaId(UUID.randomUUID().toString().split("-")[0])
        val serializedLocation = "${location.x}:${location.y}:${location.z}:${location.yaw}:${location.pitch}"
        val status = ArenaStatus.CONFIGURATING

        if (getArena(id) != null || (Blocko.instance.arenaHandler.cachedArenas.size >= Blocko.instance.globalConfigFile.gameArenaMaxParallelAmount)) return false

        transaction {
            ArenaDAO.insert { statement ->
                statement[ArenaDAO.id] = id
                statement[ArenaDAO.worldName] = worldName
                statement[playerLocation] = serializedLocation
                statement[maxPlayers] = 4
                statement[ArenaDAO.status] = status.name
            }
        }

        val idlePhase = IdlePhase(id)
        gamePhaseHandler.cachedGamePhases.put(id, idlePhase)
        gamePhaseHandler.cachedGamePhases.put(id, IngamePhase(id))
        gamePhaseHandler.cachedGamePhases.put(id, EndingPhase(id))

        this.cachedArenaIds.add(id)
        val arena = Arena(id, Bukkit.getWorld(worldName)!!, status, idlePhase, location.y, location)
        Blocko.instance.gameTeamHandler.gameTeams.putAll(arena.id, Constants.GAME_TEAMS)

        this.cachedArenas.add(arena)
        return true
    }

    fun deleteArena(arenaId: ArenaId) {
        val arenaSign = Blocko.instance.arenaSignHandler.getSign(arenaId)

        Blocko.instance.gameFieldHandler.deleteFields(arenaId)
        Blocko.instance.gameTeamHandler.deleteTeamSpawns(arenaId)
        Blocko.instance.gamePhaseHandler.deletePhases(arenaId)
        Blocko.instance.gameTeamHandler.gameTeams.removeAll(arenaId)

        transaction {
            ArenaDAO.deleteWhere { ArenaDAO.id eq arenaId }
        }

        this.cachedArenaIds.remove(arenaId)
        this.cachedArenas.removeIf { it.id == arenaId }
        this.activeIngameArenas.removeIf { it.id == arenaId }

        if (arenaSign != null) loadJoinSign(arenaSign.location, null)
    }

    fun resetArenas(shutdown: Boolean) {
        for (arena in this.cachedArenas) {
            arena.reset(shutdown)
        }

        this.activeIngameArenas.clear()

        if (shutdown) this.cachedArenas.map { it.gameWorld }.forEach { world -> world.save(true) }
    }

    fun updateIngameArenaCache(arena: Arena) {
        if (arena.phase.isIngame()) {
            this.activeIngameArenas.add(arena)
        } else {
            this.activeIngameArenas.remove(arena)
        }
    }

    fun getArenaId(value: String): ArenaId? {
        return this.cachedArenaIds.find { it.value == value }
    }

    fun getArena(id: ArenaId): Arena? {
        return this.cachedArenas.find { it.id == id }
    }

    fun getArenaOfPlayer(uuid: UUID): Arena? {
        return this.cachedArenas.find { it.currentPlayers.any { gamePlayer: GamePlayer -> gamePlayer.uuid == uuid } }
            ?: getArenaOfSpectator(uuid)
    }

    fun getArenaOfSpectator(uuid: UUID): Arena? {
        return this.cachedArenas.find { it.spectatorPlayers.contains(uuid) }
    }

    //TODO: make the sign layout configurable
    fun loadJoinSign(location: Location, arena: Arena?) {
        val block = location.block
        if (!block.type.name.contains("WALL_SIGN", true)) return

        val sign = block.state as Sign
        val signSide = sign.getSide(Side.FRONT)

        signSide.line(0, Component.text("BLOCKO", NamedTextColor.BLUE, TextDecoration.BOLD))

        if (arena == null) {
            signSide.line(1, Component.text("Searching", NamedTextColor.GRAY))
            signSide.line(2, Component.text("for arena...", NamedTextColor.GRAY))
        } else {
            val arenaStatus = arena.status
            val arenaPhase = arena.phase

            val statusLine = when (arenaStatus) {
                ArenaStatus.READY -> when (arenaPhase) {
                    is IdlePhase -> Component.text(
                        "${arena.currentPlayers.size}/${arena.teamOptions.playerCount}",
                        NamedTextColor.YELLOW
                    )

                    is IngamePhase -> Component.text("Ingame...", NamedTextColor.RED)
                    is EndingPhase -> Component.text("Ending...", NamedTextColor.RED)
                    else -> Component.text("Phase 404", NamedTextColor.RED)
                }

                ArenaStatus.CONFIGURATING -> Component.text("Configuration...", NamedTextColor.RED)
                ArenaStatus.RESETTING -> Component.text("Resetting...", NamedTextColor.RED)
            }

            signSide.line(1, Component.text(arena.teamOptions.getDisplayString(), NamedTextColor.AQUA))
            signSide.line(2, statusLine)

            if (arenaStatus == ArenaStatus.READY && arenaPhase.isIdle()) {
                signSide.line(3, Component.text("[JOIN]", NamedTextColor.GREEN))
            } else if (arenaStatus == ArenaStatus.READY && arenaPhase.isIngame()) {
                signSide.line(3, Component.text("[SPECTATE]", NamedTextColor.GOLD))
            } else {
                signSide.line(3, Component.text(" "))
            }
        }

        sign.update()
    }

}