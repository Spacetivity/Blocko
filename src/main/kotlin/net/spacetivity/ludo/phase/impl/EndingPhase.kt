package net.spacetivity.ludo.phase.impl

import net.kyori.adventure.text.Component
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.countdown.impl.EndingCountdown
import net.spacetivity.ludo.phase.GamePhase
import org.bukkit.inventory.ItemStack

class EndingPhase(arenaId: String) : GamePhase(arenaId, "ending", 2, EndingCountdown(arenaId)) {

    override fun start() {
        println("Phase $name started in arena $arenaId!")

        val gameArena: GameArena = getArena()
        gameArena.sendArenaMessage(Component.text("The game ends now..."))

        countdown?.tryStartup()
    }

    override fun stop() {
        println("Phase $name stopped in arena $arenaId!")
    }

    override fun initPhaseHotbarItems(hotbarItems: MutableMap<Int, ItemStack>) {

    }

}