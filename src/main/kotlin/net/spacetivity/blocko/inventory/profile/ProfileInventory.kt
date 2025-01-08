package net.spacetivity.blocko.inventory.profile

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.stats.StatsPlayer
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.utils.InventoryUtils
import net.spacetivity.blocko.utils.ItemBuilder
import net.spacetivity.inventory.api.inventory.InventoryController
import net.spacetivity.inventory.api.inventory.InventoryProperties
import net.spacetivity.inventory.api.inventory.InventoryProvider
import net.spacetivity.inventory.api.item.InteractiveItem
import org.bukkit.Material
import org.bukkit.entity.Player

@InventoryProperties(id = "profile_inv", rows = 6, columns = 9)
class ProfileInventory(private val isShopItemActive: Boolean) : InventoryProvider {

    override fun init(player: Player, controller: InventoryController) {
        val translation: Translation = BlockoGame.instance.translationHandler.getSelectedTranslation()

        controller.fill(InventoryController.FillType.TOP_BORDER, InteractiveItem.placeholder(Material.BLACK_STAINED_GLASS_PANE))
        controller.fill(InventoryController.FillType.BOTTOM_BORDER, InteractiveItem.placeholder(Material.BLACK_STAINED_GLASS_PANE))

        controller.setItem(2, 4, InteractiveItem.of(ItemBuilder(Material.ARMOR_STAND)
            .setName(translation.validateItemName("blocko.inventory.profile.stats_item.display_name"))
            .setLoreByComponent(translation.validateItemLore("blocko.inventory.profile.stats_item.lore"))
            .build()) { _, _, _ ->
            val statsPlayer: StatsPlayer = BlockoGame.instance.statsPlayerHandler.getStatsPlayer(player.uniqueId)
                ?: return@of
            InventoryUtils.openStatsInventory(player, statsPlayer)
        })

        val entityShopDisplayNameKey = "blocko.inventory.profile.entity_shop_item.display_name.${if (this.isShopItemActive) "active" else "not_active"}"
        val entityShopLoreKey = "blocko.inventory.profile.entity_shop_item.lore.${if (this.isShopItemActive) "active" else "not_active"}"

        controller.setItem(3, 2, InteractiveItem.of(ItemBuilder(if (this.isShopItemActive) Material.SLIME_SPAWN_EGG else Material.BARRIER)
            .setName(translation.validateItemName(entityShopDisplayNameKey))
            .setLoreByComponent(translation.validateItemLore(entityShopLoreKey))
            .build()) { _, _, _ ->
            if (!this.isShopItemActive) return@of
            InventoryUtils.openEntityShopInventory(player)
        })

        controller.setItem(3, 6, InteractiveItem.of(ItemBuilder(Material.NETHER_STAR)
            .setName(translation.validateItemName("blocko.inventory.profile.achievements_item.display_name"))
            .setLoreByComponent(translation.validateItemLore("blocko.inventory.profile.achievements_item.lore"))
            .build()) { _, _, _ -> InventoryUtils.openAchievementsInventory(player) })
    }

}