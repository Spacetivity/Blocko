package net.spacetivity.blocko.phase.impl

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.arena.getArena
import net.spacetivity.blocko.countdown.impl.IdleCountdown
import net.spacetivity.blocko.item.itemStack
import net.spacetivity.blocko.item.meta
import net.spacetivity.blocko.item.name
import net.spacetivity.blocko.item.onInteract
import net.spacetivity.blocko.phase.GamePhase
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.translation.translateMessage
import net.spacetivity.blocko.utils.InventoryUtils
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

class IdlePhase(arenaId: String) : GamePhase(arenaId, "idling", 0, IdleCountdown(arenaId)) {

    override fun start() {

    }

    override fun stop() {

    }

    override fun initPhaseHotbarItems(hotbarItems: MutableMap<Int, ItemStack>) {
        val translation: Translation = BlockoGame.instance.translationHandler.getSelectedTranslation()

        hotbarItems[0] = itemStack(Material.CLOCK) {
            meta {
                this.name = translation.displayName("blocko.items.profile.display_name")
                lore(translation.lore("blocko.items.profile.lore"))
            }
        }.onInteract { event ->
            val player: Player = event.player
            InventoryUtils.openProfileInventory(player, true)
        }

        hotbarItems[1] = itemStack(Material.RED_BED) {
            meta {
                name = translation.displayName("blocko.items.team_selector.display_name")
                lore(translation.lore("blocko.items.team_selector.lore"))
            }
        }.onInteract { event ->
            val player: Player = event.player
            val gameArena: GameArena = player.getArena() ?: return@onInteract
            InventoryUtils.openTeamSelectorInventory(player, gameArena)
        }

        hotbarItems[4] = itemStack(Material.PLAYER_HEAD) {
            meta<SkullMeta> {
                name =translation.displayName("blocko.items.instant_starter.display_name")
                lore(translation.lore("blocko.items.instant_starter.lore"))
            }
        }.onInteract { event ->
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

        hotbarItems[7] = itemStack(Material.COMPARATOR) {
            meta {
                name = translation.displayName("blocko.items.host_settings.display_name")
                lore(translation.lore("blocko.items.host_settings.lore"))
            }
        }.onInteract { event ->
            val player: Player = event.player
            val gameArena: GameArena = player.getArena() ?: return@onInteract

            if (gameArena.arenaHost!!.uuid != player.uniqueId) {
                player.translateMessage("blocko.phase.host_item_blocked")
                return@onInteract
            }

            InventoryUtils.openHostSettingsInventory(player, gameArena)
        }

        hotbarItems[8] = itemStack(Material.SLIME_BALL) {
            meta {
                name = translation.displayName("blocko.items.leave.display_name")
            }
        }.onInteract { event ->
            val player: Player = event.player
            val gameArena: GameArena = player.getArena() ?: return@onInteract
            gameArena.quit(player)
        }
    }

    override fun initSpectatorHotbarItems(hotbarItems: MutableMap<Int, ItemStack>) {

    }

}