package net.spacetivity.blocko.inventory.profile

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.getArena
import net.spacetivity.blocko.arena.toGamePlayerInstance
import net.spacetivity.blocko.item.itemStack
import net.spacetivity.blocko.item.meta
import net.spacetivity.blocko.item.name
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.utils.InventoryUtils
import net.spacetivity.inventory.api.inventory.InventoryController
import net.spacetivity.inventory.api.inventory.InventoryProperties
import net.spacetivity.inventory.api.inventory.InventoryProvider
import net.spacetivity.inventory.api.item.InteractiveItem
import net.spacetivity.inventory.api.item.InventoryPos
import org.bukkit.Material
import org.bukkit.entity.Player

@InventoryProperties(id = "achievements_inv", rows = 6, columns = 9)
class AchievementsInventory : InventoryProvider {

    override fun init(player: Player, controller: InventoryController) {
        val translation = BlockoGame.instance.translationHandler.getSelectedTranslation()

        controller.fill(InventoryController.FillType.TOP_BORDER, InteractiveItem.placeholder(Material.BLACK_STAINED_GLASS_PANE))
        controller.fill(InventoryController.FillType.BOTTOM_BORDER, InteractiveItem.placeholder(Material.BLACK_STAINED_GLASS_PANE))

        controller.setItem(0, 4, InteractiveItem.of(itemStack(Material.SLIME_BALL) {
            meta {
                name = translation.displayName("blocko.inventory_utils.back_item_display_name")
            }
        }) { _, _, _ ->
            val gameArena = player.getArena() ?: return@of
            val isShopItemActive = gameArena.phase.isIdle()
            InventoryUtils.openProfileInventory(player, isShopItemActive)
        })

        val pageItems = fetchAchievementItems(player, translation)

        if (pageItems.isEmpty()) {
            controller.fill(InventoryController.FillType.RECTANGLE, InteractiveItem.of(itemStack(Material.BARRIER) {
                meta {
                    name = translation.displayName("blocko.inventory.achievements.no_achievements_found.display_name")
                }
            }), InventoryPos.of(2, 3), InventoryPos.of(3, 5))
            return
        }

        val pagination = controller.createPagination()
        pagination.limitItemsPerPage(36)
        pagination.setItemField(1, 0, 4, 8)
        pagination.distributeItems(pageItems)

        InventoryUtils.setPreviousPageItem(5, 7, controller)
        InventoryUtils.setNextPageItem(5, 8, controller)
    }

    private fun fetchAchievementItems(player: Player, translation: Translation): List<InteractiveItem> {
        val items = mutableListOf<InteractiveItem>()

        val achievementHandler = BlockoGame.instance.achievementHandler
        val achievementPlayer = achievementHandler.getAchievementPlayer(player.uniqueId) ?: return items

        val gamePlayer = player.toGamePlayerInstance() ?: return items

        for (achievement in achievementHandler.cachedAchievements) {
            val hasCompleted = achievementPlayer.hasCompleted(achievement)

            val suffixPlaceholder = if (hasCompleted)
                Placeholder.parsed("suffix", translation.lineAsString("blocko.inventory.achievements.achievement_item.suffix"))
            else
                Placeholder.parsed("suffix", "")

            items.add(InteractiveItem.of(itemStack(if (hasCompleted) Material.LIME_DYE else Material.GRAY_DYE) {
                meta {
                    name = translation.displayName("blocko.inventory.achievements.achievement_item.display_name",
                        Placeholder.parsed("achievement_color", "<${if (hasCompleted) NamedTextColor.GREEN.asHexString() else NamedTextColor.DARK_GRAY.asHexString()}>"),
                        Placeholder.parsed("achievement_name", achievement.name),
                        suffixPlaceholder)

                    if (!hasCompleted) lore(achievement.getDescription(gamePlayer))
                }
            }))
        }

        return items
    }

}