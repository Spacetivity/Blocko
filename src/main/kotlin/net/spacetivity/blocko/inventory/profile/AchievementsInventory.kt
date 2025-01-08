package net.spacetivity.blocko.inventory.profile

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.achievement.AchievementHandler
import net.spacetivity.blocko.achievement.AchievementPlayer
import net.spacetivity.blocko.achievement.container.Achievement
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.extensions.getArena
import net.spacetivity.blocko.extensions.toGamePlayerInstance
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.utils.InventoryUtils
import net.spacetivity.blocko.utils.ItemBuilder
import net.spacetivity.inventory.api.inventory.InventoryController
import net.spacetivity.inventory.api.inventory.InventoryProperties
import net.spacetivity.inventory.api.inventory.InventoryProvider
import net.spacetivity.inventory.api.item.InteractiveItem
import net.spacetivity.inventory.api.item.InventoryPos
import net.spacetivity.inventory.api.pagination.InventoryPagination
import org.bukkit.Material
import org.bukkit.entity.Player

@InventoryProperties(id = "achievements_inv", rows = 6, columns = 9)
class AchievementsInventory : InventoryProvider {

    override fun init(player: Player, controller: InventoryController) {
        val translation: Translation = BlockoGame.instance.translationHandler.getSelectedTranslation()

        controller.fill(InventoryController.FillType.TOP_BORDER, InteractiveItem.placeholder(Material.BLACK_STAINED_GLASS_PANE))
        controller.fill(InventoryController.FillType.BOTTOM_BORDER, InteractiveItem.placeholder(Material.BLACK_STAINED_GLASS_PANE))

        controller.setItem(0, 4, InteractiveItem.of(ItemBuilder(Material.SLIME_BALL)
            .setName(translation.validateItemName("blocko.inventory_utils.back_item_display_name"))
            .build()) { _, _, _ ->
            val gameArena: GameArena = player.getArena() ?: return@of
            val isShopItemActive: Boolean = gameArena.phase.isIdle()
            InventoryUtils.openProfileInventory(player, isShopItemActive)
        })

        val pageItems: List<InteractiveItem> = fetchAchievementItems(player, translation)

        if (pageItems.isEmpty()) {
            controller.fill(InventoryController.FillType.RECTANGLE, InteractiveItem.of(ItemBuilder(Material.BARRIER)
                .setName(translation.validateItemName("blocko.inventory.achievements.no_achievements_found.display_name"))
                .build()), InventoryPos.of(2, 3), InventoryPos.of(3, 5))
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

    private fun fetchAchievementItems(player: Player, translation: Translation): List<InteractiveItem> {
        val items: MutableList<InteractiveItem> = mutableListOf()

        val achievementHandler: AchievementHandler = BlockoGame.instance.achievementHandler
        val achievementPlayer: AchievementPlayer = achievementHandler.getAchievementPlayer(player.uniqueId)
            ?: return items

        val gamePlayer: GamePlayer = player.toGamePlayerInstance() ?: return items

        for (achievement: Achievement in achievementHandler.cachedAchievements) {
            val hasCompleted: Boolean = achievementPlayer.hasCompleted(achievement)

            val suffixPlaceholder: TagResolver = if (hasCompleted)
                Placeholder.parsed("suffix", translation.validateLineAsString("blocko.inventory.achievements.achievement_item.suffix"))
            else
                Placeholder.parsed("suffix", "")

            val itemBuilder = ItemBuilder(if (hasCompleted) Material.LIME_DYE else Material.GRAY_DYE)
                .setName(translation.validateItemName("blocko.inventory.achievements.achievement_item.display_name",
                    Placeholder.parsed("achievement_color", "<${if (hasCompleted) NamedTextColor.GREEN.asHexString() else NamedTextColor.DARK_GRAY.asHexString()}>"),
                    Placeholder.parsed("achievement_name", achievement.name),
                    suffixPlaceholder))

            if (!hasCompleted) itemBuilder.setLoreByComponent(achievement.getDescription(gamePlayer))
            items.add(InteractiveItem.of(itemBuilder.build()))
        }

        return items
    }

}