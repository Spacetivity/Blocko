package net.spacetivity.ludo.countdown.impl

import net.kyori.adventure.text.Component
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.countdown.GameCountdown
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.scoreboard.GameScoreboardUtils
import net.spacetivity.ludo.team.GameTeam
import org.bukkit.Sound
import org.bukkit.entity.EntityType
import org.bukkit.scheduler.BukkitTask
import java.util.*

class IdleCountdown(arenaId: String) : GameCountdown(arenaId, 5) {

    override fun handleCountdownIdle(countdownTask: BukkitTask, remainingSeconds: Int) {
        val gameArena: GameArena = LudoGame.instance.gameArenaHandler.getArena(this.arenaId) ?: return
        val isOne = remainingSeconds == 1

        if (remainingSeconds % 10 == 0 || remainingSeconds < 6) {
            gameArena.sendArenaMessage(Component.text("Game starts in ${if (isOne) "one" else remainingSeconds} ${if (isOne) "second" else "seconds"}."))
            gameArena.sendArenaSound(Sound.ENTITY_PLAYER_LEVELUP, 0.2F)
        }

        // if (remainingSeconds == 2) addMissingPlayers(gameArena)
    }

    override fun handleCountdownEnd() {
        val gameArena: GameArena = LudoGame.instance.gameArenaHandler.getArena(this.arenaId) ?: return

        addMissingPlayers(gameArena)

        for (gamePlayer: GamePlayer in gameArena.currentPlayers) {
            if (gamePlayer.teamName != null) continue
            LudoGame.instance.gameTeamHandler.gameTeams[gameArena.id].first { it.teamMembers.isEmpty() }.join(gamePlayer)
            GameScoreboardUtils.updateTeamLine(gamePlayer)
        }

        for (gameTeamLocation in LudoGame.instance.gameTeamHandler.getLocationsOfAllTeams(this.arenaId)) {
            val gameTeam: GameTeam? = LudoGame.instance.gameTeamHandler.getTeam(gameTeamLocation.arenaId, gameTeamLocation.teamName)
            if (gameTeam == null || gameTeam.teamMembers.isEmpty()) continue
            LudoGame.instance.gameEntityHandler.spawnEntity(gameTeamLocation, EntityType.VILLAGER)
            gameTeamLocation.isTaken = true
        }

        gameArena.invitedPlayers.clear()

        LudoGame.instance.gamePhaseHandler.nextPhase(gameArena)
    }

    private fun addMissingPlayers(gameArena: GameArena) {
        val missingPlayerCount: Int = gameArena.teamOptions.playerCount - gameArena.currentPlayers.size
        if (missingPlayerCount <= 0) return

        for (i in 0..<missingPlayerCount) {
            gameArena.join(UUID.randomUUID(), true)
        }
    }

}