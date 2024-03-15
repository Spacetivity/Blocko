package net.spacetivity.ludo.phase.impl

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.inventory.api.extension.openStaticInventory
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.countdown.impl.IdleCountdown
import net.spacetivity.ludo.extensions.getArena
import net.spacetivity.ludo.inventory.host.HostSettingsInventory
import net.spacetivity.ludo.inventory.profile.ProfileInventory
import net.spacetivity.ludo.inventory.team.TeamSelectorInventory
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
            .setName(Component.text("Profile (Right-click)"))
            .onInteract { event: PlayerInteractEvent ->
                val player: Player = event.player
                LudoGame.instance.server.openStaticInventory(player, Component.text("Your profile"), ProfileInventory())
            }
            .build()

        hotbarItems[1] = ItemBuilder(Material.RED_BED)
            .setName("Team Selector (Right-click)")
            .setLoreByComponent(mutableListOf(Component.text("Choose a team for yourself")))
            .onInteract { event: PlayerInteractEvent ->
                val player: Player = event.player
                val gameArena: GameArena = player.getArena() ?: return@onInteract

                val title: Component = LudoGame.instance.translationHandler.getSelectedTranslation().validateLine("blocko.inventory.team_selector.title")
                LudoGame.instance.server.openStaticInventory(player, title, TeamSelectorInventory(gameArena))
            }
            .build()

        hotbarItems[7] = ItemBuilder(Material.COMPARATOR)
            .setName("Host Settings (Right-click)")
            .setLoreByComponent(mutableListOf(Component.text("Change the game settings")))
            .onInteract { event: PlayerInteractEvent ->
                val player: Player = event.player
                val gameArena: GameArena = player.getArena() ?: return@onInteract

                if (gameArena.arenaHost!!.uuid != player.uniqueId) {
                    player.sendMessage(Component.text("You have to be the host to edit the arena settings!", NamedTextColor.RED))
                    return@onInteract
                }

                val title: Component = LudoGame.instance.translationHandler.getSelectedTranslation().validateLine("blocko.inventory.host.title")
                LudoGame.instance.server.openStaticInventory(player, title, HostSettingsInventory(gameArena))
            }
            .build()

        hotbarItems[8] = ItemBuilder(Material.SLIME_BALL)
            .setName(Component.text("Leave (Right-click)"))
            .setLoreByComponent(mutableListOf(Component.text("Leave the game")))
            .build()
    }

}