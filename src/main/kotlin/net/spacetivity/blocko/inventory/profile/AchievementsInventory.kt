package net.spacetivity.blocko.inventory.profile

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.Blocko
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
import org.bukkit.Material
import org.bukkit.entity.Player

@GuiProperties(id = "achievements_inv", rows = 6, columns = 9)
class AchievementsInventory : Gui {

    override fun init(player: Player, controller: GuiController) {
        val translation = Blocko.instance.translationHandler.getSelectedTranslation()

        controller.fill(GuiController.FillType.TOP_BORDER, GuiProvider.api.placeholder(Material.BLACK_STAINED_GLASS_PANE))
        controller.fill(GuiController.FillType.BOTTOM_BORDER, GuiProvider.api.placeholder(Material.BLACK_STAINED_GLASS_PANE))

        controller.setItem(0, 4, GuiProvider.api.of(itemStack(Material.SLIME_BALL) {
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
            controller.fill(GuiController.FillType.RECTANGLE, GuiProvider.api.of(itemStack(Material.BARRIER) {
                meta {
                    name = translation.displayName("blocko.inventory.achievements.no_achievements_found.display_name")
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

    private fun fetchAchievementItems(player: Player, translation: Translation): List<GuiItem> {
        val items = mutableListOf<GuiItem>()

        val achievementHandler = Blocko.instance.achievementHandler
        val achievementPlayer = achievementHandler.getAchievementPlayer(player.uniqueId) ?: return items

        val gamePlayer = player.toGamePlayerInstance() ?: return items

        for (achievement in achievementHandler.cachedAchievements) {
            val hasCompleted = achievementPlayer.hasCompleted(achievement)

            val suffixPlaceholder = if (hasCompleted)
                Placeholder.parsed("suffix", translation.lineAsString("blocko.inventory.achievements.achievement_item.suffix"))
            else
                Placeholder.parsed("suffix", "")

            items.add(GuiProvider.api.of(itemStack(if (hasCompleted) Material.LIME_DYE else Material.GRAY_DYE) {
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