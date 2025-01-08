package net.spacetivity.blocko.phase

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.extensions.playSound
import net.spacetivity.blocko.phase.impl.IngamePhase
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.scoreboard.GameScoreboardUtils
import net.spacetivity.blocko.team.GameTeam
import net.spacetivity.blocko.translation.Translation
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.time.Duration

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
            gameArena.reset(false)
            val translation: Translation = BlockoGame.instance.translationHandler.getSelectedTranslation()
            Bukkit.getConsoleSender().sendMessage(translation.validateLine("blocko.phase.not_found",
                Placeholder.parsed("priority", newPhasePriority.toString()),
                Placeholder.parsed("id", gameArena.id)))
            return
        }

        if (newGamePhase is IngamePhase) {
            val availableTeams: List<GameTeam> = BlockoGame.instance.gameTeamHandler.gameTeams[gameArena.id].filter { it.teamMembers.isNotEmpty() }
            val smallestTeamId: Int? = availableTeams.minOfOrNull { it.teamId }

            newGamePhase.controllingTeamId = availableTeams.filter { it.teamId == smallestTeamId }.random().teamId
            newGamePhase.lastControllingTeamId = newGamePhase.controllingTeamId

            val controllingTeam: GameTeam = newGamePhase.getControllingTeam() ?: return
            GameScoreboardUtils.updateControllingTeamLine(gameArena, controllingTeam)

            val controllingPlayer: GamePlayer? = gameArena.currentPlayers.find { it.uuid == controllingTeam.teamMembers.first() }

            if (controllingPlayer != null) {
                if (controllingPlayer.actionTimeoutTimestamp == null) controllingPlayer.actionTimeoutTimestamp = System.currentTimeMillis() + Duration.ofMinutes(1).toMillis()
                controllingPlayer.playSound(Sound.BLOCK_NOTE_BLOCK_PLING)
            }
        }

        gameArena.phase = newGamePhase
        newGamePhase.start()

        BlockoGame.instance.gameArenaSignHandler.updateArenaSign(gameArena)
    }

    fun initIndexPhase(gameArena: GameArena) {
        gameArena.phase.stop()

        val indexPhase: GamePhase = this.cachedGamePhases[gameArena.id].find { it.priority == 0 } ?: return
        gameArena.phase = indexPhase
        indexPhase.start()
    }

}