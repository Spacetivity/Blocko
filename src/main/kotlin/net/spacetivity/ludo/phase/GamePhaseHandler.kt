package net.spacetivity.ludo.phase

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.ludo.arena.GameArena
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

class GamePhaseHandler {

    val cachedGamePhases: Multimap<String, GamePhase> = ArrayListMultimap.create()

    fun deletePhases(arenaId: String) {
        this.cachedGamePhases.removeAll(arenaId)
    }

    fun nextPhase(gameArena: GameArena) {
        for (uuid: UUID in gameArena.currentPlayers) {
            val player: Player = Bukkit.getPlayer(uuid) ?: return
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