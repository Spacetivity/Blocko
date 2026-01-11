package net.spacetivity.blocko.inventory.profile

import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.item.itemStack
import net.spacetivity.blocko.item.meta
import net.spacetivity.blocko.item.name
import net.spacetivity.blocko.utils.InventoryUtils
import net.spacetivity.inventorylib.api.GuiProvider
import net.spacetivity.inventorylib.api.inventory.GuiController
import net.spacetivity.inventorylib.api.inventory.GuiProperties
import net.spacetivity.inventorylib.api.inventory.Gui
import net.spacetivity.inventorylib.api.item.GuiItem
import org.bukkit.Material
import org.bukkit.entity.Player

@GuiProperties(id = "profile_inv", rows = 6, columns = 9)
class ProfileInventory(private val isShopItemActive: Boolean) : Gui {

    override fun init(player: Player, controller: GuiController) {
        val translation = Blocko.instance.translationHandler.getSelectedTranslation()

        controller.fill(GuiController.FillType.TOP_BORDER, GuiProvider.api.placeholder(Material.BLACK_STAINED_GLASS_PANE))
        controller.fill(GuiController.FillType.BOTTOM_BORDER, GuiProvider.api.placeholder(Material.BLACK_STAINED_GLASS_PANE))

        controller.setItem(2, 4, GuiProvider.api.of(itemStack(Material.ARMOR_STAND) {
            meta {
                name = translation.displayName("blocko.inventory.profile.stats_item.display_name")
                lore(translation.lore("blocko.inventory.profile.stats_item.lore"))
            }
        }) { _, _, _ ->
            val statsPlayer = Blocko.instance.statsPlayerHandler.getStatsPlayer(player.uniqueId) ?: return@of
            InventoryUtils.openStatsInventory(player, statsPlayer)
        })

        val entityShopDisplayNameKey = "blocko.inventory.profile.entity_shop_item.display_name.${if (this.isShopItemActive) "active" else "not_active"}"
        val entityShopLoreKey = "blocko.inventory.profile.entity_shop_item.lore.${if (this.isShopItemActive) "active" else "not_active"}"

        controller.setItem(3, 2, GuiProvider.api.of(itemStack(if (this.isShopItemActive) Material.SLIME_SPAWN_EGG else Material.BARRIER) {
            meta {
                name = translation.displayName(entityShopDisplayNameKey)
                lore(translation.lore(entityShopLoreKey))
            }
        }) { _, _, _ ->
            if (!this.isShopItemActive) return@of
            InventoryUtils.openEntityShopInventory(player)
        })

        controller.setItem(3, 6, GuiProvider.api.of(itemStack(Material.NETHER_STAR) {
            meta {
                name = translation.displayName("blocko.inventory.profile.achievements_item.display_name")
                lore(translation.lore("blocko.inventory.profile.achievements_item.lore"))
            }
        }) { _, _, _ -> InventoryUtils.openAchievementsInventory(player) })
    }

}