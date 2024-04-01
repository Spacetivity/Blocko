package net.spacetivity.blocko.countdown.impl

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.countdown.GameCountdown
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.scoreboard.GameScoreboardUtils
import net.spacetivity.blocko.team.GameTeam
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.*

class IdleCountdown(arenaId: String) : GameCountdown(arenaId, BlockoGame.instance.globalConfigFile.idleCountdownSeconds) {

    override fun handleCountdownIdle(countdownTask: BukkitTask, remainingSeconds: Int) {
        val gameArena: GameArena = BlockoGame.instance.gameArenaHandler.getArena(this.arenaId) ?: return
        val isOne = remainingSeconds == 1

        if (remainingSeconds % 10 == 0 || remainingSeconds < 6) {
            gameArena.sendArenaSound(Sound.ENTITY_PLAYER_LEVELUP, 0.2F)
            gameArena.sendArenaMessage("blocko.countdown.idle.running",
                Placeholder.parsed("time", (if (isOne) "one" else remainingSeconds).toString()),
                Placeholder.parsed("time_string", if (isOne) "second" else "seconds"))
        }
    }

    override fun handleCountdownEnd() {
        val gameArena: GameArena = BlockoGame.instance.gameArenaHandler.getArena(this.arenaId) ?: return

        for (gamePlayer: GamePlayer in gameArena.currentPlayers.filter { !it.isAI }) {
            val player: Player = gamePlayer.toBukkitInstance() ?: continue
            player.teleport(gameArena.location)
            player.allowFlight = true
            player.isFlying = true
        }

        addMissingPlayers(gameArena)

        for (gamePlayer: GamePlayer in gameArena.currentPlayers) {
            if (gamePlayer.teamName != null) continue
            BlockoGame.instance.gameTeamHandler.gameTeams[gameArena.id].first { it.teamMembers.isEmpty() }.join(gamePlayer)
        }

        for (player: Player in gameArena.getAllPlayers()) GameScoreboardUtils.updateTeamLine(player)

        for (gameTeamLocation in BlockoGame.instance.gameTeamHandler.getLocationsOfAllTeams(this.arenaId)) {
            val gameTeam: GameTeam? = BlockoGame.instance.gameTeamHandler.getTeam(gameTeamLocation.arenaId, gameTeamLocation.teamName)
            if (gameTeam == null || gameTeam.teamMembers.isEmpty()) continue
            val gamePlayer: GamePlayer = gameArena.currentPlayers.find { it.uuid == gameTeam.teamMembers.first() } ?: continue
            BlockoGame.instance.gameEntityHandler.spawnEntity(gameTeamLocation, gamePlayer.selectedEntityType)
            gameTeamLocation.isTaken = true
        }

        gameArena.invitedPlayers.clear()

        BlockoGame.instance.gamePhaseHandler.nextPhase(gameArena)
    }

    private fun addMissingPlayers(gameArena: GameArena) {
        val missingPlayerCount: Int = gameArena.teamOptions.playerCount - gameArena.currentPlayers.size
        if (missingPlayerCount <= 0) return

        for (i in 0..<missingPlayerCount) {
            gameArena.join(UUID.randomUUID(), true)
        }
    }

}