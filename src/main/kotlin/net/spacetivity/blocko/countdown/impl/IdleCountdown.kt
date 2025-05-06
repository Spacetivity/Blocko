package net.spacetivity.blocko.countdown.impl

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.arena.Arena
import net.spacetivity.blocko.arena.id.ArenaId
import net.spacetivity.blocko.countdown.GameCountdown
import net.spacetivity.blocko.scoreboard.GameScoreboardUtils
import org.bukkit.Sound
import org.bukkit.scheduler.BukkitTask
import java.util.*

class IdleCountdown(arenaId: ArenaId) : GameCountdown(arenaId, Blocko.instance.globalConfigFile.idleCountdownSeconds) {

    override fun handleCountdownIdle(countdownTask: BukkitTask, remainingSeconds: Int) {
        val gameArena = Blocko.instance.arenaHandler.getArena(this.arenaId) ?: return
        val isOne = remainingSeconds == 1

        if (remainingSeconds % 10 == 0 || remainingSeconds < 6) {
            gameArena.sendArenaSound(Sound.ENTITY_PLAYER_LEVELUP, 0.2F)
            gameArena.sendArenaMessage("blocko.countdown.idle.running",
                Placeholder.parsed("time", (if (isOne) "one" else remainingSeconds).toString()),
                Placeholder.parsed("time_string", if (isOne) "second" else "seconds"))
        }
    }

    override fun handleCountdownEnd() {
        val gameArena = Blocko.instance.arenaHandler.getArena(this.arenaId) ?: return

        for (gamePlayer in gameArena.currentPlayers.filter { !it.isAI }) {
            val player = gamePlayer.toBukkitInstance() ?: continue
            player.teleport(gameArena.location)
            player.allowFlight = true
            player.isFlying = true
        }

        addMissingPlayers(gameArena)

        for (gamePlayer in gameArena.currentPlayers) {
            if (gamePlayer.teamName != null) continue
            Blocko.instance.gameTeamHandler.gameTeams[gameArena.id].first { it.teamMembers.isEmpty() }.join(gamePlayer)
        }

        for (player in gameArena.getAllPlayers()) GameScoreboardUtils.updateTeamLine(player)

        for (gameTeamLocation in Blocko.instance.gameTeamHandler.getLocationsOfAllTeams(this.arenaId)) {
            val gameTeam = Blocko.instance.gameTeamHandler.getTeam(gameTeamLocation.arenaId, gameTeamLocation.teamName)
            if (gameTeam == null || gameTeam.teamMembers.isEmpty()) continue
            val gamePlayer = gameArena.currentPlayers.find { it.uuid == gameTeam.teamMembers.first() } ?: continue
            Blocko.instance.gameEntityHandler.spawnEntity(gameTeamLocation, gamePlayer.selectedEntityType)
            gameTeamLocation.isTaken = true
        }

        gameArena.invitedPlayers.clear()

        Blocko.instance.gamePhaseHandler.nextPhase(gameArena)
    }

    private fun addMissingPlayers(arena: Arena) {
        val missingPlayerCount = arena.teamOptions.playerCount - arena.currentPlayers.size
        if (missingPlayerCount <= 0) return

        for (i in 0..<missingPlayerCount) {
            arena.join(UUID.randomUUID(), true)
        }
    }

}