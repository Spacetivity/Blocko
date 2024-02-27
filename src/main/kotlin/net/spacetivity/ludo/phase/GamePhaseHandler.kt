package net.spacetivity.ludo.phase

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.phase.impl.IngamePhase
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.team.GameTeam
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class GamePhaseHandler {

    val cachedGamePhases: Multimap<String, GamePhase> = ArrayListMultimap.create()

    fun deletePhases(arenaId: String) {
        this.cachedGamePhases.removeAll(arenaId)
    }

    fun nextPhase(gameArena: GameArena) {
        for (gamePlayer: GamePlayer in gameArena.currentPlayers.filter { !it.isAI }) {
            val player: Player = gamePlayer.toBukkitInstance() ?: continue
            gameArena.phase.clearPlayerInventory(player)
        }

        gameArena.phase.stop()

        val newPhasePriority: Int = gameArena.phase.priority.inc()
        val newGamePhase: GamePhase? = this.cachedGamePhases[gameArena.id].find { it.priority == newPhasePriority }

        if (newGamePhase == null) {
            gameArena.reset()
            Bukkit.getConsoleSender().sendMessage(Component.text("ERROR: Phase $newPhasePriority not found for arena ${gameArena.id}!", NamedTextColor.DARK_RED))
            return
        }

        if (newGamePhase is IngamePhase) {
            val availableTeams: List<GameTeam> = LudoGame.instance.gameTeamHandler.gameTeams[gameArena.id].filter { it.teamMembers.isNotEmpty() }
            val smallestTeamId: Int? = availableTeams.minOfOrNull { it.teamId }

            newGamePhase.controllingTeamId = availableTeams.filter { it.teamId == smallestTeamId }.random().teamId
            newGamePhase.lastControllingTeamId = newGamePhase.controllingTeamId
        }

        gameArena.phase = newGamePhase
        newGamePhase.start()
    }

    fun initIndexPhase(gameArena: GameArena) {
        gameArena.phase.stop()

        val indexPhase: GamePhase = this.cachedGamePhases[gameArena.id].find { it.priority == 0 } ?: return
        gameArena.phase = indexPhase
        indexPhase.start()
    }

}