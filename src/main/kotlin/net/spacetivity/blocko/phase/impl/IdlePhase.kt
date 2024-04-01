package net.spacetivity.blocko.phase.impl

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.countdown.impl.IdleCountdown
import net.spacetivity.blocko.extensions.getArena
import net.spacetivity.blocko.extensions.translateMessage
import net.spacetivity.blocko.phase.GamePhase
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.utils.HeadUtils
import net.spacetivity.blocko.utils.InventoryUtils
import net.spacetivity.blocko.utils.ItemBuilder
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

class IdlePhase(arenaId: String) : GamePhase(arenaId, "idling", 0, IdleCountdown(arenaId)) {

    override fun start() {

    }

    override fun stop() {

    }

    override fun initPhaseHotbarItems(hotbarItems: MutableMap<Int, ItemStack>) {
        val translation: Translation = BlockoGame.instance.translationHandler.getSelectedTranslation()

        hotbarItems[0] = ItemBuilder(Material.CLOCK)
            .setName(translation.validateItemName("blocko.items.profile.display_name"))
            .setLoreByComponent(translation.validateItemLore("blocko.items.profile.lore"))
            .onInteract { event: PlayerInteractEvent ->
                val player: Player = event.player
                InventoryUtils.openProfileInventory(player, true)
            }
            .build()

        hotbarItems[1] = ItemBuilder(Material.RED_BED)
            .setName(translation.validateItemName("blocko.items.team_selector.display_name"))
            .setLoreByComponent(translation.validateItemLore("blocko.items.team_selector.lore"))
            .onInteract { event: PlayerInteractEvent ->
                val player: Player = event.player
                val gameArena: GameArena = player.getArena() ?: return@onInteract
                InventoryUtils.openTeamSelectorInventory(player, gameArena)
            }
            .build()

        hotbarItems[4] = ItemBuilder(Material.PLAYER_HEAD)
            .setName(translation.validateItemName("blocko.items.instant_starter.display_name"))
            .setLoreByComponent(translation.validateItemLore("blocko.items.instant_starter.lore"))
            .setOwner(HeadUtils.PLAY)
            .onInteract { event: PlayerInteractEvent ->
                val player: Player = event.player
                val gameArena: GameArena = player.getArena() ?: return@onInteract

                if (gameArena.arenaHost!!.uuid != player.uniqueId) {
                    player.translateMessage("blocko.phase.host_item_blocked")
                    return@onInteract
                }

                if (this.countdown == null) return@onInteract
                if (this.countdown!!.modifiableDuration <= 5) return@onInteract

                gameArena.waitForActualPlayers = false

                if (!this.countdown!!.isRunning) this.countdown!!.tryStartup()
                this.countdown!!.modifiableDuration = 5

                player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 0.1F)
            }
            .build()

        hotbarItems[7] = ItemBuilder(Material.COMPARATOR)
            .setName(translation.validateItemName("blocko.items.host_settings.display_name"))
            .setLoreByComponent(translation.validateItemLore("blocko.items.host_settings.lore"))
            .onInteract { event: PlayerInteractEvent ->
                val player: Player = event.player
                val gameArena: GameArena = player.getArena() ?: return@onInteract

                if (gameArena.arenaHost!!.uuid != player.uniqueId) {
                    player.translateMessage("blocko.phase.host_item_blocked")
                    return@onInteract
                }

                InventoryUtils.openHostSettingsInventory(player, gameArena)
            }
            .build()

        hotbarItems[8] = ItemBuilder(Material.SLIME_BALL)
            .setName(translation.validateItemName("blocko.items.leave.display_name"))
            .onInteract { event: PlayerInteractEvent ->
                val player: Player = event.player
                val gameArena: GameArena = player.getArena() ?: return@onInteract
                gameArena.quit(player)
            }
            .build()
    }

    override fun initSpectatorHotbarItems(hotbarItems: MutableMap<Int, ItemStack>) {

    }

}