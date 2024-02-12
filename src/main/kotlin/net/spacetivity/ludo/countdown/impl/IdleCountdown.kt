package net.spacetivity.ludo.countdown.impl

import net.kyori.adventure.text.Component
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.countdown.GameCountdown
import net.spacetivity.ludo.team.GameTeam
import org.bukkit.Sound
import org.bukkit.entity.EntityType
import org.bukkit.scheduler.BukkitTask
import java.util.function.Predicate

class IdleCountdown(arenaId: String) : GameCountdown(arenaId, 20, Predicate { t -> t >= 1 }) {

    override fun handleCountdownIdle(countdownTask: BukkitTask, remainingSeconds: Int) {
        val gameArena: GameArena = LudoGame.instance.gameArenaHandler.getArena(this.arenaId) ?: return
        val isOne = remainingSeconds == 1

        if (remainingSeconds % 10 == 0 || remainingSeconds < 6) {
            gameArena.sendArenaMessage(Component.text("Game starts in ${if (isOne) "one" else remainingSeconds} ${if (isOne) "second" else "seconds"}."))
            gameArena.sendArenaSound(Sound.ENTITY_PLAYER_LEVELUP)
        }
    }

    override fun handleCountdownEnd() {
        // Spawns in all game entities for all teams if they have game players!
        for (gameTeamLocation in LudoGame.instance.gameTeamHandler.getLocationsOfAllTeams(this.arenaId)) {
            val gameTeam: GameTeam? = LudoGame.instance.gameTeamHandler.getTeam(gameTeamLocation.arenaId, gameTeamLocation.teamName)
            if (gameTeam == null || gameTeam.teamMembers.isEmpty()) continue

            LudoGame.instance.gameEntityHandler.spawnEntity(gameTeamLocation, EntityType.VILLAGER)
        }

        val gameArena: GameArena = LudoGame.instance.gameArenaHandler.getArena(this.arenaId) ?: return
        LudoGame.instance.gamePhaseHandler.nextPhase(gameArena)
    }

}