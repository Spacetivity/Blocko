package net.spacetivity.ludo.inventory.host

import com.destroystokyo.paper.profile.ProfileProperty
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.inventory.api.annotation.InventoryProperties
import net.spacetivity.inventory.api.inventory.InventoryController
import net.spacetivity.inventory.api.inventory.InventoryProvider
import net.spacetivity.inventory.api.item.InteractiveItem
import net.spacetivity.inventory.api.item.InventoryPosition
import net.spacetivity.inventory.api.pagination.InventoryPagination
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.extensions.getArena
import net.spacetivity.ludo.extensions.toGamePlayerInstance
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.translation.Translation
import net.spacetivity.ludo.utils.InventoryUtils
import net.spacetivity.ludo.utils.ItemBuilder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player

@InventoryProperties(id = "invitation_inv", rows = 6, columns = 9)
class InvitationInventory(private val gameArena: GameArena) : InventoryProvider {

    override fun init(player: Player, controller: InventoryController) {
        val translation: Translation = LudoGame.instance.translationHandler.getSelectedTranslation()

        controller.fill(InventoryController.FillType.TOP_BORDER, InteractiveItem.placeholder(Material.BLACK_STAINED_GLASS_PANE))
        controller.fill(InventoryController.FillType.BOTTOM_BORDER, InteractiveItem.placeholder(Material.BLACK_STAINED_GLASS_PANE))

        controller.setItem(0, 4, InteractiveItem.of(ItemBuilder(Material.SLIME_BALL)
            .setName(translation.validateItemName("blocko.inventory_utils.back_item_display_name"))
            .build()) { _, _, _ -> InventoryUtils.openHostSettingsInventory(player, gameArena) })

        val pageItems: List<InteractiveItem> = fetchPlayerItems(player, translation)

        if (pageItems.isEmpty()) {
            controller.fill(InventoryController.FillType.RECTANGLE, InteractiveItem.of(ItemBuilder(Material.BARRIER)
                .setName(translation.validateItemName("blocko.inventory.invitation.no_players_to_invite_found.display_name"))
                .setLoreByComponent(translation.validateItemLore("blocko.inventory.invitation.no_players_to_invite_found.lore"))
                .build()), InventoryPosition.of(1, 0), InventoryPosition.of(4, 8))
            return
        }

        val pagination: InventoryPagination = controller.createPagination()
        pagination.limitItemsPerPage(36)
        pagination.setItemField(1, 0, 4, 8)
        pagination.distributeItems(pageItems)

        controller.setItem(5, 7, InteractiveItem.previousPage(ItemBuilder(Material.ARROW)
            .setName(translation.validateItemName("blocko.inventory_utils.previous_page_item_display_name"))
            .build(), pagination))

        controller.setItem(5, 8, InteractiveItem.nextPage(ItemBuilder(Material.SPECTRAL_ARROW)
            .setName(translation.validateItemName("blocko.inventory_utils.next_page_item_display_name"))
            .build(), pagination))
    }

    private fun fetchPlayerItems(host: Player, translation: Translation): List<InteractiveItem> {
        val hostGamePlayer: GamePlayer = host.toGamePlayerInstance() ?: return emptyList()

        val items: MutableList<InteractiveItem> = mutableListOf()

        for (player: Player in Bukkit.getOnlinePlayers().filter { it.name != host.name && it.getArena() == null }) {
            val property: ProfileProperty = player.playerProfile.properties.first()

            items.add(InteractiveItem.of(ItemBuilder(Material.PLAYER_HEAD)
                .setName(translation.validateItemName("blocko.inventory.invitation.player_head.display_name", Placeholder.parsed("name", player.name)))
                .setLoreByComponent(translation.validateItemLore("blocko.inventory.invitation.player_head.lore"))
                .setOwner(property.value)
                .build()) { _, _, _ ->
                this.gameArena.sendArenaInvite(hostGamePlayer, player.name)
                host.playSound(host.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F)
            })
        }

        return items
    }

}