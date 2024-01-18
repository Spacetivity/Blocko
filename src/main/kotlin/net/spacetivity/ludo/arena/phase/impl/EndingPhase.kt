package net.spacetivity.ludo.arena.phase.impl

import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.arena.phase.GamePhase
import org.bukkit.inventory.ItemStack

class EndingPhase(arenaId: String) : GamePhase(arenaId, "ending", 2) {

    override fun start() {
        val gameArena: GameArena = getArena()
        //TODO: Start kick player scheduler and after it reset the arena 'gameArena.reset()'
    }

    override fun stop() {
        TODO("Not yet implemented")
    }

    override fun initPhaseHotbarItems(hotbarItems: MutableMap<Int, ItemStack>) {
        TODO("Not yet implemented")
    }

}