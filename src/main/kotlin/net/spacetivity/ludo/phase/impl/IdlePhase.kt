package net.spacetivity.ludo.phase.impl

import net.kyori.adventure.text.Component
import net.spacetivity.ludo.countdown.impl.IdleCountdown
import net.spacetivity.ludo.phase.GamePhase
import net.spacetivity.ludo.utils.ItemBuilder
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class IdlePhase(arenaId: String) : GamePhase(arenaId, "idling", 0, IdleCountdown(arenaId)) {

    override fun start() {
        println("Phase $name started in arena ${this.arenaId}!")
    }

    override fun stop() {
        println("Phase $name stopped in arena ${this.arenaId}")
    }

    override fun initPhaseHotbarItems(hotbarItems: MutableMap<Int, ItemStack>) {
        hotbarItems[0] = ItemBuilder(Material.ALLAY_SPAWN_EGG)
            .setName(Component.text("Entity Menu [Right-click]"))
            .setLoreByComponent(mutableListOf(Component.text("Select and buy entities with your gained coins")))
            .build()

        hotbarItems[1] = ItemBuilder(Material.COMPARATOR)
            .setName(Component.text("Host Settings [Right-click]"))
            .setLoreByComponent(mutableListOf(Component.text("If you are the Game-Host, you"), Component.text("can control the arena here.")))
            .build()

        hotbarItems[8] = ItemBuilder(Material.SLIME_BALL)
            .setName(Component.text("Leave Game [Right-click]"))
            .setLoreByComponent(mutableListOf(Component.text("Leave the game")))
            .build()
    }

}