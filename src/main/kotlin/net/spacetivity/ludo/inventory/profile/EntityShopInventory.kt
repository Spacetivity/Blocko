package net.spacetivity.ludo.inventory.profile

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.inventory.api.annotation.InventoryProperties
import net.spacetivity.inventory.api.inventory.InventoryController
import net.spacetivity.inventory.api.inventory.InventoryProvider
import net.spacetivity.inventory.api.item.InteractiveItem
import net.spacetivity.inventory.api.item.InventoryPosition
import net.spacetivity.inventory.api.pagination.InventoryPagination
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.achievement.AchievementPlayer
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.entity.GameEntityType
import net.spacetivity.ludo.extensions.getArena
import net.spacetivity.ludo.stats.StatsPlayer
import net.spacetivity.ludo.translation.Translation
import net.spacetivity.ludo.utils.InventoryUtils
import net.spacetivity.ludo.utils.ItemBuilder
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

@InventoryProperties(id = "entity_shop_inv", rows = 6, columns = 9)
class EntityShopInventory : InventoryProvider {

    override fun init(player: Player, controller: InventoryController) {
        val translation: Translation = LudoGame.instance.translationHandler.getSelectedTranslation()

        controller.fill(InventoryController.FillType.TOP_BORDER, InteractiveItem.placeholder(Material.BLACK_STAINED_GLASS_PANE))
        controller.fill(InventoryController.FillType.BOTTOM_BORDER, InteractiveItem.placeholder(Material.BLACK_STAINED_GLASS_PANE))

        controller.setItem(0, 4, InteractiveItem.of(ItemBuilder(Material.SLIME_BALL)
            .setName(translation.validateItemName("blocko.inventory_utils.back_item_display_name"))
            .build()) { _, _, _ ->
            val gameArena: GameArena = player.getArena() ?: return@of
            val isShopItemActive: Boolean = gameArena.phase.isIdle()
            InventoryUtils.openProfileInventory(player, isShopItemActive)
        })

        val pageItems: List<InteractiveItem> = fetchEntityTypeItems(controller, player, translation)

        if (pageItems.isEmpty()) {
            controller.fill(InventoryController.FillType.RECTANGLE, InteractiveItem.of(ItemBuilder(Material.BARRIER)
                .setName(translation.validateItemName("blocko.inventory.entity_shop.no_entity_types_found.display_name"))
                .build()), InventoryPosition.of(2, 3), InventoryPosition.of(3, 5))
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

    private fun fetchEntityTypeItems(controller: InventoryController, player: Player, translation: Translation): List<InteractiveItem> {
        val items: MutableList<InteractiveItem> = mutableListOf()

        for (gameEntityType: GameEntityType in GameEntityType.entries) {
            items.add(InteractiveItem.of(ItemBuilder(buildEntityTypeItemType(player, gameEntityType))
                .setName(buildEntityTypeDisplayName(translation, player, gameEntityType))
                .setLoreByComponent(buildEntityTypeItemLore(translation, player, LudoGame.instance.statsPlayerHandler.getStatsPlayer(player.uniqueId)!!, gameEntityType))
                .build()) { _, item: InteractiveItem, event: InventoryClickEvent ->

                if (LudoGame.instance.gameEntityHandler.hasUnlockedEntityType(player.uniqueId, gameEntityType))
                    return@of

                val achievementPlayer: AchievementPlayer? = LudoGame.instance.achievementHandler.getAchievementPlayer(player.uniqueId)
                if (gameEntityType.neededAchievementKey != null && achievementPlayer != null && !achievementPlayer.achievementNames.contains(gameEntityType.neededAchievementKey))
                    return@of

                val statsPlayer: StatsPlayer = LudoGame.instance.statsPlayerHandler.getStatsPlayer(player.uniqueId)!!

                if (statsPlayer.coins < gameEntityType.price)
                    return@of

                val playerWhoClicked: Player = event.whoClicked as Player

                gameEntityType.buyEntityType(playerWhoClicked)

                item.update(controller, InteractiveItem.Modification.TYPE, buildEntityTypeItemType(player,gameEntityType))
                item.update(controller, InteractiveItem.Modification.DISPLAY_NAME, buildEntityTypeDisplayName(translation, playerWhoClicked, gameEntityType))
                item.update(controller, InteractiveItem.Modification.LORE, buildEntityTypeItemLore(translation, player, statsPlayer, gameEntityType))
            })

        }

        return items
    }

    private fun buildEntityTypeItemType(player: Player, gameEntityType: GameEntityType): Material{
        val isUnlocked: Boolean = gameEntityType.isUnlockedByPlayer(player.uniqueId)
        return if (isUnlocked) gameEntityType.getSpawnEggType()!! else Material.BARRIER
    }

    private fun buildEntityTypeDisplayName(translation: Translation, player: Player, gameEntityType: GameEntityType): Component {
        val isUnlocked: Boolean = gameEntityType.isUnlockedByPlayer(player.uniqueId)
        val statusColor: NamedTextColor = if (isUnlocked) NamedTextColor.GREEN else NamedTextColor.DARK_GRAY

        val displayNameSuffixPlaceholder = if (isUnlocked) Placeholder.parsed("suffix",
            translation.validateLineAsString("blocko.inventory.entity_shop.entity_type_item.suffix"))
        else
            Placeholder.parsed("suffix", "")

        return translation.validateItemName("blocko.inventory.entity_shop.entity_type_item.display_name",
            Placeholder.parsed("status_color", "<${statusColor.asHexString()}>"),
            Placeholder.parsed("entity_type_name", gameEntityType.bukkitEntityType.name),
            displayNameSuffixPlaceholder)
    }

    private fun buildEntityTypeItemLore(translation: Translation, player: Player, statsPlayer: StatsPlayer, gameEntityType: GameEntityType): List<Component> {
        val isUnlocked: Boolean = gameEntityType.isUnlockedByPlayer(player.uniqueId)
        val loreKey = "blocko.inventory.entity_shop.entity_type_item.lore.${if (isUnlocked) "active" else "not_active"}"

        val possibleAchievementPlaceholder: TagResolver.Single = if (gameEntityType.neededAchievementKey == null) Placeholder.parsed("possible_achievement_name", "-/-")
        else Placeholder.parsed("possible_achievement_name", LudoGame.instance.achievementHandler.getAchievementByKey(gameEntityType.neededAchievementKey)?.name
            ?: "-/-")

        val loreSuffixPlaceholder: TagResolver.Single = if (isUnlocked)
            Placeholder.parsed("lore_suffix", "")
        else if (gameEntityType.price > statsPlayer.coins)
            Placeholder.parsed("lore_suffix", translation.validateLineAsString("blocko.inventory.entity_shop.entity_type_item.lore.suffix.not_buyable"))
        else
            Placeholder.parsed("lore_suffix", translation.validateLineAsString("blocko.inventory.entity_shop.entity_type_item.lore.suffix.buyable"))

        return translation.validateItemLore(loreKey,
            Placeholder.parsed("price", gameEntityType.price.toString()),
            possibleAchievementPlaceholder,
            loreSuffixPlaceholder)
    }

}