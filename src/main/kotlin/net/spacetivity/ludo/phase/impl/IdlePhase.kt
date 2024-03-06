package net.spacetivity.ludo.phase.impl

import net.kyori.adventure.text.Component
import net.spacetivity.inventory.api.extension.openStaticInventory
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.countdown.impl.IdleCountdown
import net.spacetivity.ludo.inventory.ProfileInventory
import net.spacetivity.ludo.phase.GamePhase
import net.spacetivity.ludo.utils.ItemBuilder
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

class IdlePhase(arenaId: String) : GamePhase(arenaId, "idling", 0, IdleCountdown(arenaId)) {

    override fun start() {

    }

    override fun stop() {

    }

    override fun initPhaseHotbarItems(hotbarItems: MutableMap<Int, ItemStack>) {
        hotbarItems[0] = ItemBuilder(Material.CLOCK)
            .setName(Component.text("Profile [Right-click]"))
            .onInteract { event: PlayerInteractEvent ->
                val player: Player = event.player
                LudoGame.instance.server.openStaticInventory(player, Component.text("Your profile"), ProfileInventory())
            }
            .build()

        hotbarItems[8] = ItemBuilder(Material.SLIME_BALL)
            .setName(Component.text("Leave Game [Right-click]"))
            .setLoreByComponent(mutableListOf(Component.text("Leave the game")))
            .build()
    }

}