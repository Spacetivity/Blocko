package net.spacetivity.blocko.inventory.profile

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.achievement.getAchievementByKey
import net.spacetivity.blocko.arena.getArena
import net.spacetivity.blocko.arena.toGamePlayerInstance
import net.spacetivity.blocko.entity.GameEntityType
import net.spacetivity.blocko.item.*
import net.spacetivity.blocko.stats.StatsPlayer
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.translation.translateMessage
import net.spacetivity.blocko.utils.Constants
import net.spacetivity.blocko.utils.Constants.BALANCE_ITEM_KEY
import net.spacetivity.blocko.utils.Constants.GAME_ENTITY_TYPE_KEY
import net.spacetivity.blocko.utils.InventoryUtils
import net.spacetivity.blocko.utils.NumberUtils
import net.spacetivity.blocko.utils.PersistentDataUtils
import net.spacetivity.inventorylib.api.GuiProvider
import net.spacetivity.inventorylib.api.inventory.Gui
import net.spacetivity.inventorylib.api.inventory.GuiController
import net.spacetivity.inventorylib.api.inventory.GuiProperties
import net.spacetivity.inventorylib.api.item.GuiItem
import net.spacetivity.inventorylib.api.item.GuiPos
import net.spacetivity.inventorylib.api.pagination.GuiPagination
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player

@GuiProperties(id = "entity_shop_inv", rows = 6, columns = 9)
class EntityShopInventory : Gui {

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

        val pagination = controller.createPagination()
        val pageItems = fetchEntityTypeItems(controller, pagination, player, translation)

        if (pageItems.isEmpty()) {
            controller.fill(GuiController.FillType.RECTANGLE, GuiProvider.api.of(itemStack(Material.BARRIER) {
                meta {
                    name = translation.displayName("blocko.inventory.entity_shop.no_entity_types_found.display_name")
                }
            }), GuiPos.of(2, 3), GuiPos.of(3, 5))
            return
        }

        pagination.limitItemsPerPage(36)
        pagination.setItemField(1, 0, 4, 8)
        pagination.distributeItems(pageItems)

        controller.setItem(5, 0, GuiProvider.api.of(itemStack(Material.RAW_GOLD) {
            meta {
                name = buildBalanceDisplayName(translation, player)
                applyPersistentData(BALANCE_ITEM_KEY, true)
            }
        }))

        InventoryUtils.setPreviousPageItem(5, 7, controller)
        InventoryUtils.setNextPageItem(5, 8, controller)
    }

    private fun fetchEntityTypeItems(controller: GuiController, pagination: GuiPagination, player: Player, translation: Translation): List<GuiItem> {
        val items = mutableListOf<GuiItem>()
        val gamePlayer = player.toGamePlayerInstance() ?: return items

        for (gameEntityType in GameEntityType.entries) {
            val entityItemStack = itemStack(buildEntityTypeItemType(player, gameEntityType)) {
                meta {
                    name = buildEntityTypeDisplayName(translation, player, gameEntityType)
                    lore(buildEntityTypeItemLore(translation, player, Blocko.instance.statsPlayerHandler.getStatsPlayer(player.uniqueId)!!, gameEntityType))
                    hideExtraInfo()
                    applyPersistentData(GAME_ENTITY_TYPE_KEY, gameEntityType.name)

                    if (gamePlayer.selectedEntityType == gameEntityType) addUnsafeEnchantment(Enchantment.UNBREAKING, 1)
                }
            }

            items.add(GuiProvider.api.of(entityItemStack) { _, item, event ->
                val playerWhoClicked = event.whoClicked as Player

                if (Blocko.instance.gameEntityHandler.hasUnlockedEntityType(player.uniqueId, gameEntityType) && playerWhoClicked.toGamePlayerInstance()!!.selectedEntityType != gameEntityType) {
                    val gamePlayerWoClicked = playerWhoClicked.toGamePlayerInstance() ?: return@of

                    val oldSelectedEntityType = gamePlayerWoClicked.selectedEntityType
                    val oldEntityTypeItem = pagination.getPaginationItems()
                        .filter { PersistentDataUtils.has(it.item.itemMeta, GAME_ENTITY_TYPE_KEY) }
                        .find { PersistentDataUtils.get(it.item.itemMeta, GAME_ENTITY_TYPE_KEY, String::class.java) == gamePlayerWoClicked.selectedEntityType.name }

                    gamePlayerWoClicked.selectedEntityType = gameEntityType

                    playerWhoClicked.playSound(playerWhoClicked.location, Sound.BLOCK_NOTE_BLOCK_PLING, 10F, 1F)
                    playerWhoClicked.translateMessage("blocko.entity_shop.selected_entity_type", Placeholder.parsed("entity_type_name", gameEntityType.getCorrectedTypeName()))

                    item.update(controller, GuiItem.Modification.DISPLAY_NAME, buildEntityTypeDisplayName(translation, playerWhoClicked, gameEntityType))
                    setEntityTypeItemGlow(playerWhoClicked, controller, item, gameEntityType)

                    oldEntityTypeItem?.update(controller, GuiItem.Modification.DISPLAY_NAME, buildEntityTypeDisplayName(translation, playerWhoClicked, oldSelectedEntityType))
                    if (oldEntityTypeItem != null) {
                        setEntityTypeItemGlow(playerWhoClicked, controller, oldEntityTypeItem, oldSelectedEntityType)
                    }
                    return@of
                }

                val achievementPlayer = Blocko.instance.achievementHandler.getAchievementPlayer(player.uniqueId)

                val properties = gameEntityType.getProperties()
                val requiresAchievement = properties.requiresAchievement()

                if (requiresAchievement && achievementPlayer != null && !achievementPlayer.achievementNames.contains(properties.achievementKey))
                    return@of

                val statsPlayer = Blocko.instance.statsPlayerHandler.getStatsPlayer(player.uniqueId)!!

                if (statsPlayer.coins < properties.price)
                    return@of

                gameEntityType.buyEntityType(playerWhoClicked)

                val balanceItem = controller.contents.values.firstOrNull { it != null && PersistentDataUtils.has(it.item.itemMeta, BALANCE_ITEM_KEY) }
                balanceItem?.update(controller, GuiItem.Modification.DISPLAY_NAME, buildBalanceDisplayName(translation, player))

                setEntityTypeItemGlow(playerWhoClicked, controller, item, gameEntityType)
                item.update(controller, GuiItem.Modification.TYPE, buildEntityTypeItemType(player, gameEntityType))
                item.update(controller, GuiItem.Modification.DISPLAY_NAME, buildEntityTypeDisplayName(translation, playerWhoClicked, gameEntityType))
                item.update(controller, GuiItem.Modification.LORE, buildEntityTypeItemLore(translation, player, statsPlayer, gameEntityType))
            })

        }

        return items
    }

    private fun buildBalanceDisplayName(translation: Translation, player: Player): Component {
        val statsPlayer = Blocko.instance.statsPlayerHandler.getStatsPlayer(player.uniqueId)!!
        return translation.displayName("blocko.inventory.entity_shop.balance_item.display_name", Placeholder.parsed("amount", NumberUtils.format(statsPlayer.coins)))
    }

    private fun setEntityTypeItemGlow(player: Player, controller: GuiController, guiItem: GuiItem, gameEntityType: GameEntityType) {
        val gamePlayer = player.toGamePlayerInstance() ?: return
        val isSelected = gamePlayer.selectedEntityType == gameEntityType

        if (isSelected) Blocko.instance.gameEntityHandler.setSelectedEntityType(player.uniqueId, gameEntityType)

        guiItem.update(controller, GuiItem.Modification.GLOWING, isSelected)
    }

    private fun buildEntityTypeItemType(player: Player, gameEntityType: GameEntityType): Material {
        val isUnlocked = gameEntityType.isUnlockedByPlayer(player.uniqueId)
        return if (isUnlocked) gameEntityType.getSpawnEggType() else Material.BARRIER
    }

    private fun buildEntityTypeDisplayName(translation: Translation, player: Player, gameEntityType: GameEntityType): Component {
        val isUnlocked = gameEntityType.isUnlockedByPlayer(player.uniqueId)
        val statusColor = if (isUnlocked) NamedTextColor.GREEN else NamedTextColor.DARK_GRAY

        val gamePlayer = player.toGamePlayerInstance() ?: return Component.text("")
        val isSelected = gamePlayer.selectedEntityType == gameEntityType

        val displayNameSuffixPlaceholder = if (isUnlocked) Placeholder.parsed("suffix",
            translation.lineAsString("blocko.inventory.entity_shop.entity_type_item.suffix.${if (isSelected) "selected" else "unlocked"}"))
        else
            Placeholder.parsed("suffix", "")

        return translation.displayName("blocko.inventory.entity_shop.entity_type_item.display_name",
            Placeholder.parsed("status_color", "<${statusColor.asHexString()}>"),
            Placeholder.parsed("entity_type_name", gameEntityType.getCorrectedTypeName()),
            displayNameSuffixPlaceholder)
    }

    private fun buildEntityTypeItemLore(translation: Translation, player: Player, statsPlayer: StatsPlayer, gameEntityType: GameEntityType): List<Component> {
        val isUnlocked = gameEntityType.isUnlockedByPlayer(player.uniqueId)
        val loreKey = "blocko.inventory.entity_shop.entity_type_item.lore.${if (isUnlocked) "active" else "not_active"}"

        val properties = gameEntityType.getProperties()

        val possibleAchievementPlaceholder: TagResolver.Single = if (!properties.requiresAchievement()) Placeholder.parsed("possible_achievement_name", Constants.PLACEHOLDER)
        else Placeholder.parsed("possible_achievement_name", getAchievementByKey(properties.achievementKey)?.name
            ?: ":=)")

        val loreSuffixPlaceholder: TagResolver.Single = if (isUnlocked)
            Placeholder.parsed("lore_suffix", "")
        else if (properties.price > statsPlayer.coins)
            Placeholder.parsed("lore_suffix", translation.lineAsString("blocko.inventory.entity_shop.entity_type_item.lore.suffix.not_buyable"))
        else
            Placeholder.parsed("lore_suffix", translation.lineAsString("blocko.inventory.entity_shop.entity_type_item.lore.suffix.buyable"))

        return translation.lore(loreKey,
            Placeholder.parsed("price", NumberUtils.format(properties.price)),
            possibleAchievementPlaceholder,
            loreSuffixPlaceholder)
    }

}