package net.spacetivity.blocko.phase

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.arena.Arena
import net.spacetivity.blocko.arena.id.ArenaId
import net.spacetivity.blocko.phase.impl.IngamePhase
import net.spacetivity.blocko.player.playSound
import net.spacetivity.blocko.scoreboard.ScoreboardUtils
import org.bukkit.Bukkit
import org.bukkit.Sound
import java.time.Duration

class GamePhaseHandler {

    val cachedGamePhases: Multimap<ArenaId, GamePhase> = ArrayListMultimap.create()

    fun deletePhases(arenaId: ArenaId) {
        this.cachedGamePhases.removeAll(arenaId)
    }

    fun nextPhase(arena: Arena) {
        for (gamePlayer in arena.currentPlayers.filter { !it.isAI }) {
            val player = gamePlayer.toBukkitInstance() ?: continue
            arena.phase.clearPlayerInventory(player)
        }

        arena.phase.stop()

        val newPhasePriority = arena.phase.priority.inc()
        val newGamePhase = this.cachedGamePhases[arena.id].find { it.priority == newPhasePriority }

        if (newGamePhase == null) {
            arena.reset(false)
            val translation = Blocko.instance.translationHandler.getSelectedTranslation()
            Bukkit.getConsoleSender().sendMessage(translation.line("blocko.phase.not_found",
                Placeholder.parsed("priority", newPhasePriority.toString()),
                Placeholder.parsed("id", arena.id.value)))
            return
        }

        if (newGamePhase is IngamePhase) {
            val availableTeams = Blocko.instance.gameTeamHandler.gameTeams[arena.id].filter { it.teamMembers.isNotEmpty() }
            val smallestTeamId = availableTeams.minOfOrNull { it.teamId }

            newGamePhase.controllingTeamId = availableTeams.filter { it.teamId == smallestTeamId }.random().teamId
            newGamePhase.lastControllingTeamId = newGamePhase.controllingTeamId

            val controllingTeam = newGamePhase.getControllingTeam() ?: return
            ScoreboardUtils.updateControllingTeamLine(arena, controllingTeam)

            val controllingPlayer = arena.currentPlayers.find { it.uuid == controllingTeam.teamMembers.first() }

            if (controllingPlayer != null) {
                if (controllingPlayer.actionTimeoutTimestamp == null) controllingPlayer.actionTimeoutTimestamp = System.currentTimeMillis() + Duration.ofMinutes(1).toMillis()
                controllingPlayer.playSound(Sound.BLOCK_NOTE_BLOCK_PLING)
            }
        }

        arena.phase = newGamePhase
        newGamePhase.start()

        Blocko.instance.arenaSignHandler.updateArenaSign(arena)
    }

    fun initIndexPhase(arena: Arena) {
        arena.phase.stop()

        val indexPhase = this.cachedGamePhases[arena.id].find { it.priority == 0 } ?: return
        arena.phase = indexPhase
        indexPhase.start()
    }

}