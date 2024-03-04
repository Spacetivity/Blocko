package net.spacetivity.ludo.inventory

import net.spacetivity.inventory.api.annotation.InventoryProperties
import net.spacetivity.inventory.api.inventory.InventoryController
import net.spacetivity.inventory.api.inventory.InventoryProvider
import net.spacetivity.inventory.api.item.InteractiveItem
import net.spacetivity.inventory.api.item.InventoryPosition
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.translation.Translation
import net.spacetivity.ludo.utils.ItemBuilder
import org.bukkit.Material
import org.bukkit.entity.Player

@InventoryProperties(id = "profile_inv", rows = 6, columns = 9)
class ProfileInventory : InventoryProvider {

    override fun init(player: Player, controller: InventoryController) {
        val translation: Translation = LudoGame.instance.translationHandler.getSelectedTranslation()

        controller.fill(InventoryController.FillType.ROW, InteractiveItem.placeholder(Material.BLACK_STAINED_GLASS_PANE), InventoryPosition.of(0, 0))
        controller.fill(InventoryController.FillType.ROW, InteractiveItem.placeholder(Material.BLACK_STAINED_GLASS_PANE), InventoryPosition.of(5, 0))

        controller.setItem(2, 4, InteractiveItem.navigator(ItemBuilder(Material.ARMOR_STAND)
            .setName(translation.validateItemName("blocko.profile_inv.stats.item.displayName"))
            .build(), "stats_inv"))

        controller.setItem(3, 2, InteractiveItem.navigator(ItemBuilder(Material.SLIME_SPAWN_EGG)
            .setName(translation.validateItemName("blocko.profile_inv.entity_shop.item.displayName"))
            .build(), "entity_shop_inv"))

        controller.setItem(3, 4, InteractiveItem.navigator(ItemBuilder(Material.NETHER_STAR)
            .setName(translation.validateItemName("blocko.profile_inv.achievements.item.displayName"))
            .build(), "achievements_inv"))

        controller.setItem(3, 6, InteractiveItem.navigator(ItemBuilder(Material.COMPARATOR)
            .setName(translation.validateItemName("blocko.profile_inv.settings.item.displayName"))
            .build(), "settings_inv"))
    }

}