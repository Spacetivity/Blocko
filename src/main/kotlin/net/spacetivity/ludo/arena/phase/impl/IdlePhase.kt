package net.spacetivity.ludo.arena.phase.impl

import net.kyori.adventure.text.Component
import net.spacetivity.ludo.arena.phase.GamePhase
import net.spacetivity.ludo.utils.ItemUtils
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class IdlePhase(arenaId: String) : GamePhase(arenaId, "idling", 0) {

    override fun start() {
        println("Phase $name started in arena $arenaId!")
    }

    override fun stop() {
        println("Phase $name stopped in arena $arenaId!")
    }

    override fun initPhaseHotbarItems(hotbarItems: MutableMap<Int, ItemStack>) {
        hotbarItems[0] = ItemUtils(Material.ALLAY_SPAWN_EGG)
            .setName(Component.text("Entity Menu [Right-click]"))
            .setLoreByComponent(mutableListOf(Component.text("Select and buy entities with your gained coins")))
            .build()

        hotbarItems[1] = ItemUtils(Material.COMPARATOR)
            .setName(Component.text("Host Settings [Right-click]"))
            .setLoreByComponent(mutableListOf(Component.text("If you are the Game-Host, you"), Component.text("can control the arena here.")))
            .build()

        hotbarItems[8] = ItemUtils(Material.SLIME_BALL)
            .setName(Component.text("Leave Game [Right-click]"))
            .setLoreByComponent(mutableListOf(Component.text("Leave the game")))
            .build()
    }

}