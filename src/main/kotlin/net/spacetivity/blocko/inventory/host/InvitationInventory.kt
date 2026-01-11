package net.spacetivity.blocko.inventory.host

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.arena.Arena
import net.spacetivity.blocko.arena.getArena
import net.spacetivity.blocko.arena.toGamePlayerInstance
import net.spacetivity.blocko.item.itemStack
import net.spacetivity.blocko.item.meta
import net.spacetivity.blocko.item.name
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.utils.InventoryUtils
import net.spacetivity.inventorylib.api.GuiProvider
import net.spacetivity.inventorylib.api.inventory.GuiController
import net.spacetivity.inventorylib.api.inventory.GuiProperties
import net.spacetivity.inventorylib.api.inventory.Gui
import net.spacetivity.inventorylib.api.item.GuiItem
import net.spacetivity.inventorylib.api.item.GuiPos
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.SkullMeta

@GuiProperties(id = "invitation_inv", rows = 6, columns = 9)
class InvitationInventory(private val arena: Arena) : Gui {

    override fun init(player: Player, controller: GuiController) {
        val translation = Blocko.instance.translationHandler.getSelectedTranslation()

        controller.fill(GuiController.FillType.TOP_BORDER, GuiProvider.api.placeholder(Material.BLACK_STAINED_GLASS_PANE))
        controller.fill(GuiController.FillType.BOTTOM_BORDER, GuiProvider.api.placeholder(Material.BLACK_STAINED_GLASS_PANE))

        controller.setItem(0, 4, GuiProvider.api.of(itemStack(Material.SLIME_BALL) {
            meta {
                name = translation.displayName("blocko.inventory_utils.back_item_display_name")
            }
        }) { _, _, _ -> InventoryUtils.openHostSettingsInventory(player, arena) })

        val pageItems = fetchPlayerItems(player, translation)

        if (pageItems.isEmpty()) {
            controller.fill(GuiController.FillType.RECTANGLE, GuiProvider.api.of(itemStack(Material.BARRIER) {
                meta {
                    name = translation.displayName("blocko.inventory.invitation.no_players_to_invite_found.display_name")
                    lore(translation.lore("blocko.inventory.invitation.no_players_to_invite_found.lore"))
                }
            }), GuiPos.of(2, 3), GuiPos.of(3, 5))
            return
        }

        val pagination = controller.createPagination()
        pagination.limitItemsPerPage(36)
        pagination.setItemField(1, 0, 4, 8)
        pagination.distributeItems(pageItems)

        InventoryUtils.setPreviousPageItem(5, 7, controller)
        InventoryUtils.setNextPageItem(5, 8, controller)
    }

    private fun fetchPlayerItems(host: Player, translation: Translation): List<GuiItem> {
        val hostGamePlayer = host.toGamePlayerInstance() ?: return emptyList()

        val items = mutableListOf<GuiItem>()

        for (player in Bukkit.getOnlinePlayers().filter { it.name != host.name && it.getArena() == null }) {
            items.add(GuiProvider.api.of(itemStack(Material.PLAYER_HEAD) {
                meta<SkullMeta> {
                    name = translation.displayName("blocko.inventory.invitation.player_head.display_name", Placeholder.parsed("name", player.name))
                    lore(translation.lore("blocko.inventory.invitation.player_head.lore"))
                    playerProfile = player.playerProfile
                }
            }) { _, _, _ ->
                this.arena.sendArenaInvite(hostGamePlayer, player.name)
                host.playSound(host.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F)
            })
        }

        return items
    }

}