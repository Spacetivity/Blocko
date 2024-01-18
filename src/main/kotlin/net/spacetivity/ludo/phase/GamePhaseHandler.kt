package net.spacetivity.ludo.phase

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import net.spacetivity.ludo.arena.GameArena

class GamePhaseHandler {

    val cachedGamePhases: Multimap<String, GamePhase> = ArrayListMultimap.create()

    fun nextPhase(gameArena: GameArena) {
        gameArena.phase.stop()

        val newPhasePriority: Int = gameArena.phase.priority.inc()
        val newGamePhase: GamePhase = this.cachedGamePhases[gameArena.id].find { it.priority == newPhasePriority }
            ?: throw NullPointerException("Phase $newPhasePriority not found for arena ${gameArena.id}!")

        gameArena.phase = newGamePhase
        newGamePhase.start()
        println("Arena ${gameArena.id} has now changed its phase to: ${newGamePhase.name}")
    }

    fun initIndexPhase(gameArena: GameArena) {
        gameArena.phase.stop()

        val indexPhase: GamePhase = this.cachedGamePhases[gameArena.id].find { it.priority == 0 } ?: return
        gameArena.phase = indexPhase
        indexPhase.start()
    }

}