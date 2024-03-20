package net.spacetivity.blocko.inventory.profile

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.achievement.AchievementPlayer
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.entity.GameEntityType
import net.spacetivity.blocko.extensions.getArena
import net.spacetivity.blocko.extensions.toGamePlayerInstance
import net.spacetivity.blocko.extensions.translateMessage
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.stats.StatsPlayer
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.utils.InventoryUtils
import net.spacetivity.blocko.utils.ItemBuilder
import net.spacetivity.blocko.utils.NumberUtils
import net.spacetivity.blocko.utils.PersistentDataUtils
import net.spacetivity.inventory.api.annotation.InventoryProperties
import net.spacetivity.inventory.api.inventory.InventoryController
import net.spacetivity.inventory.api.inventory.InventoryProvider
import net.spacetivity.inventory.api.item.InteractiveItem
import net.spacetivity.inventory.api.item.InventoryPosition
import net.spacetivity.inventory.api.pagination.InventoryPagination
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemFlag

@InventoryProperties(id = "entity_shop_inv", rows = 6, columns = 9)
class EntityShopInventory : InventoryProvider {

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

        val pagination: InventoryPagination = controller.createPagination()

        val pageItems: List<InteractiveItem> = fetchEntityTypeItems(controller, pagination, player, translation)

        if (pageItems.isEmpty()) {
            controller.fill(InventoryController.FillType.RECTANGLE, InteractiveItem.of(ItemBuilder(Material.BARRIER)
                .setName(translation.validateItemName("blocko.inventory.entity_shop.no_entity_types_found.display_name"))
                .build()), InventoryPosition.of(2, 3), InventoryPosition.of(3, 5))
            return
        }

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

    private fun fetchEntityTypeItems(controller: InventoryController, pagination: InventoryPagination, player: Player, translation: Translation): List<InteractiveItem> {
        val items: MutableList<InteractiveItem> = mutableListOf()
        val gamePlayer: GamePlayer = player.toGamePlayerInstance() ?: return items

        for (gameEntityType: GameEntityType in GameEntityType.entries) {
            val itemBuilder: ItemBuilder = ItemBuilder(buildEntityTypeItemType(player, gameEntityType))
                .setName(buildEntityTypeDisplayName(translation, player, gameEntityType))
                .setLoreByComponent(buildEntityTypeItemLore(translation, player, BlockoGame.instance.statsPlayerHandler.getStatsPlayer(player.uniqueId)!!, gameEntityType))
                .addFlags(ItemFlag.HIDE_ENCHANTS)
                .setData("gameEntityType", gameEntityType.name)

            if (gamePlayer.selectedEntityType == gameEntityType)
                itemBuilder.addGlow()

            items.add(InteractiveItem.of(itemBuilder.build()) { _, item: InteractiveItem, event: InventoryClickEvent ->

                val playerWhoClicked: Player = event.whoClicked as Player

                if (BlockoGame.instance.gameEntityHandler.hasUnlockedEntityType(player.uniqueId, gameEntityType) && playerWhoClicked.toGamePlayerInstance()!!.selectedEntityType != gameEntityType) {
                    val gamePlayerWoClicked: GamePlayer = playerWhoClicked.toGamePlayerInstance() ?: return@of

                    val oldSelectedEntityType: GameEntityType = gamePlayerWoClicked.selectedEntityType
                    val oldEntityTypeItem: InteractiveItem? = pagination.getPaginationItems()
                        .filter { PersistentDataUtils.hasData(it.item.itemMeta, "gameEntityType") }
                        .find { PersistentDataUtils.getData(it.item.itemMeta, "gameEntityType", String::class.java) == gamePlayerWoClicked.selectedEntityType.name }

                    gamePlayerWoClicked.selectedEntityType = gameEntityType
                    playerWhoClicked.playSound(playerWhoClicked.location, Sound.BLOCK_NOTE_BLOCK_PLING, 10F, 1F)


                    playerWhoClicked.translateMessage("blocko.entity_shop.selected_entity_type", Placeholder.parsed("entity_type_name", gameEntityType.getCorrectedTypeName()))

                    item.update(controller, InteractiveItem.Modification.DISPLAY_NAME, buildEntityTypeDisplayName(translation, playerWhoClicked, gameEntityType))
                    setEntityTypeItemGlow(playerWhoClicked, controller, item, gameEntityType)

                    oldEntityTypeItem?.update(controller, InteractiveItem.Modification.DISPLAY_NAME, buildEntityTypeDisplayName(translation, playerWhoClicked, oldSelectedEntityType))
                    if (oldEntityTypeItem != null) {
                        setEntityTypeItemGlow(playerWhoClicked, controller, oldEntityTypeItem, oldSelectedEntityType)
                    }
                    return@of
                }

                val achievementPlayer: AchievementPlayer? = BlockoGame.instance.achievementHandler.getAchievementPlayer(player.uniqueId)
                if (gameEntityType.neededAchievementKey != null && achievementPlayer != null && !achievementPlayer.achievementNames.contains(gameEntityType.neededAchievementKey))
                    return@of

                val statsPlayer: StatsPlayer = BlockoGame.instance.statsPlayerHandler.getStatsPlayer(player.uniqueId)!!

                if (statsPlayer.coins < gameEntityType.price)
                    return@of

                gameEntityType.buyEntityType(playerWhoClicked)

                setEntityTypeItemGlow(playerWhoClicked, controller, item, gameEntityType)
                item.update(controller, InteractiveItem.Modification.TYPE, buildEntityTypeItemType(player, gameEntityType))
                item.update(controller, InteractiveItem.Modification.DISPLAY_NAME, buildEntityTypeDisplayName(translation, playerWhoClicked, gameEntityType))
                item.update(controller, InteractiveItem.Modification.LORE, buildEntityTypeItemLore(translation, player, statsPlayer, gameEntityType))
            })

        }

        return items
    }

    private fun setEntityTypeItemGlow(player: Player, controller: InventoryController, interactiveItem: InteractiveItem, gameEntityType: GameEntityType) {
        val gamePlayer: GamePlayer = player.toGamePlayerInstance() ?: return
        val isSelected: Boolean = gamePlayer.selectedEntityType == gameEntityType

        interactiveItem.update(controller, InteractiveItem.Modification.GLOWING, isSelected)
    }

    private fun buildEntityTypeItemType(player: Player, gameEntityType: GameEntityType): Material {
        val isUnlocked: Boolean = gameEntityType.isUnlockedByPlayer(player.uniqueId)
        return if (isUnlocked) gameEntityType.getSpawnEggType() else Material.BARRIER
    }

    private fun buildEntityTypeDisplayName(translation: Translation, player: Player, gameEntityType: GameEntityType): Component {
        val isUnlocked: Boolean = gameEntityType.isUnlockedByPlayer(player.uniqueId)
        val statusColor: NamedTextColor = if (isUnlocked) NamedTextColor.GREEN else NamedTextColor.DARK_GRAY

        val gamePlayer: GamePlayer = player.toGamePlayerInstance() ?: return Component.text("")
        val isSelected: Boolean = gamePlayer.selectedEntityType == gameEntityType

        val displayNameSuffixPlaceholder = if (isUnlocked) Placeholder.parsed("suffix",
            translation.validateLineAsString("blocko.inventory.entity_shop.entity_type_item.suffix.${if (isSelected) "selected" else "unlocked"}"))
        else
            Placeholder.parsed("suffix", "")

        return translation.validateItemName("blocko.inventory.entity_shop.entity_type_item.display_name",
            Placeholder.parsed("status_color", "<${statusColor.asHexString()}>"),
            Placeholder.parsed("entity_type_name", gameEntityType.getCorrectedTypeName()),
            displayNameSuffixPlaceholder)
    }

    private fun buildEntityTypeItemLore(translation: Translation, player: Player, statsPlayer: StatsPlayer, gameEntityType: GameEntityType): List<Component> {
        val isUnlocked: Boolean = gameEntityType.isUnlockedByPlayer(player.uniqueId)
        val loreKey = "blocko.inventory.entity_shop.entity_type_item.lore.${if (isUnlocked) "active" else "not_active"}"

        val possibleAchievementPlaceholder: TagResolver.Single = if (gameEntityType.neededAchievementKey == null) Placeholder.parsed("possible_achievement_name", "-/-")
        else Placeholder.parsed("possible_achievement_name", BlockoGame.instance.achievementHandler.getAchievementByKey(gameEntityType.neededAchievementKey)?.name
            ?: "-/-")

        val loreSuffixPlaceholder: TagResolver.Single = if (isUnlocked)
            Placeholder.parsed("lore_suffix", "")
        else if (gameEntityType.price > statsPlayer.coins)
            Placeholder.parsed("lore_suffix", translation.validateLineAsString("blocko.inventory.entity_shop.entity_type_item.lore.suffix.not_buyable"))
        else
            Placeholder.parsed("lore_suffix", translation.validateLineAsString("blocko.inventory.entity_shop.entity_type_item.lore.suffix.buyable"))

        return translation.validateItemLore(loreKey,
            Placeholder.parsed("price", NumberUtils.format(gameEntityType.price)),
            possibleAchievementPlaceholder,
            loreSuffixPlaceholder)
    }

}