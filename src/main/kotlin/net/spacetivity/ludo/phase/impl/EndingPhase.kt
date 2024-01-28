package net.spacetivity.ludo.phase.impl

import net.spacetivity.ludo.phase.GamePhase
import org.bukkit.inventory.ItemStack

class EndingPhase(arenaId: String) : GamePhase(arenaId, "ending", 2, null) {

    override fun start() {
        //TODO: Start kick player scheduler and after it reset the arena 'gameArena.reset()'
    }

    override fun stop() {

    }

    override fun initPhaseHotbarItems(hotbarItems: MutableMap<Int, ItemStack>) {

    }

}