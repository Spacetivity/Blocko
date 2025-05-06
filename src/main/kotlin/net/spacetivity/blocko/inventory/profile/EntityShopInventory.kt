package net.spacetivity.blocko.inventory.profile

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.achievement.getAchievementByClass
import net.spacetivity.blocko.arena.getArena
import net.spacetivity.blocko.arena.toGamePlayerInstance
import net.spacetivity.blocko.entity.GameEntityType
import net.spacetivity.blocko.item.*
import net.spacetivity.blocko.stats.StatsPlayer
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.translation.translateMessage
import net.spacetivity.blocko.utils.Constants.BALANCE_ITEM_KEY
import net.spacetivity.blocko.utils.Constants.GAME_ENTITY_TYPE_KEY
import net.spacetivity.blocko.utils.InventoryUtils
import net.spacetivity.blocko.utils.NumberUtils
import net.spacetivity.blocko.utils.PersistentDataUtils
import net.spacetivity.inventory.api.inventory.InventoryController
import net.spacetivity.inventory.api.inventory.InventoryProperties
import net.spacetivity.inventory.api.inventory.InventoryProvider
import net.spacetivity.inventory.api.item.InteractiveItem
import net.spacetivity.inventory.api.item.InventoryPos
import net.spacetivity.inventory.api.pagination.InventoryPagination
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player

@InventoryProperties(id = "entity_shop_inv", rows = 6, columns = 9)
class EntityShopInventory : InventoryProvider {

    override fun init(player: Player, controller: InventoryController) {
        val translation = Blocko.instance.translationHandler.getSelectedTranslation()

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

        val pagination = controller.createPagination()
        val pageItems = fetchEntityTypeItems(controller, pagination, player, translation)

        if (pageItems.isEmpty()) {
            controller.fill(InventoryController.FillType.RECTANGLE, InteractiveItem.of(itemStack(Material.BARRIER) {
                meta {
                    name = translation.displayName("blocko.inventory.entity_shop.no_entity_types_found.display_name")
                }
            }), InventoryPos.of(2, 3), InventoryPos.of(3, 5))
            return
        }

        pagination.limitItemsPerPage(36)
        pagination.setItemField(1, 0, 4, 8)
        pagination.distributeItems(pageItems)

        controller.setItem(5, 0, InteractiveItem.of(itemStack(Material.RAW_GOLD) {
            meta {
                name = buildBalanceDisplayName(translation, player)
                applyPersistentData(BALANCE_ITEM_KEY, true)
            }
        }))

        InventoryUtils.setPreviousPageItem(5, 7, controller)
        InventoryUtils.setNextPageItem(5, 8, controller)
    }

    private fun fetchEntityTypeItems(controller: InventoryController, pagination: InventoryPagination, player: Player, translation: Translation): List<InteractiveItem> {
        val items = mutableListOf<InteractiveItem>()
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

            items.add(InteractiveItem.of(entityItemStack) { _, item, event ->
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

                    item.update(controller, InteractiveItem.Modification.DISPLAY_NAME, buildEntityTypeDisplayName(translation, playerWhoClicked, gameEntityType))
                    setEntityTypeItemGlow(playerWhoClicked, controller, item, gameEntityType)

                    oldEntityTypeItem?.update(controller, InteractiveItem.Modification.DISPLAY_NAME, buildEntityTypeDisplayName(translation, playerWhoClicked, oldSelectedEntityType))
                    if (oldEntityTypeItem != null) {
                        setEntityTypeItemGlow(playerWhoClicked, controller, oldEntityTypeItem, oldSelectedEntityType)
                    }
                    return@of
                }

                val achievementPlayer = Blocko.instance.achievementHandler.getAchievementPlayer(player.uniqueId)
                if (gameEntityType.achievementClass != null && achievementPlayer != null && !achievementPlayer.achievementNames.contains(getAchievementByClass(gameEntityType.achievementClass)!!.translationKey))
                    return@of

                val statsPlayer = Blocko.instance.statsPlayerHandler.getStatsPlayer(player.uniqueId)!!

                if (statsPlayer.coins < gameEntityType.price)
                    return@of

                gameEntityType.buyEntityType(playerWhoClicked)

                val balanceItem = controller.contents.values.firstOrNull { it != null && PersistentDataUtils.has(it.item.itemMeta, BALANCE_ITEM_KEY) }
                balanceItem?.update(controller, InteractiveItem.Modification.DISPLAY_NAME, buildBalanceDisplayName(translation, player))

                setEntityTypeItemGlow(playerWhoClicked, controller, item, gameEntityType)
                item.update(controller, InteractiveItem.Modification.TYPE, buildEntityTypeItemType(player, gameEntityType))
                item.update(controller, InteractiveItem.Modification.DISPLAY_NAME, buildEntityTypeDisplayName(translation, playerWhoClicked, gameEntityType))
                item.update(controller, InteractiveItem.Modification.LORE, buildEntityTypeItemLore(translation, player, statsPlayer, gameEntityType))
            })

        }

        return items
    }

    private fun buildBalanceDisplayName(translation: Translation, player: Player): Component {
        val statsPlayer = Blocko.instance.statsPlayerHandler.getStatsPlayer(player.uniqueId)!!
        return translation.displayName("blocko.inventory.entity_shop.balance_item.display_name", Placeholder.parsed("amount", NumberUtils.format(statsPlayer.coins)))
    }

    private fun setEntityTypeItemGlow(player: Player, controller: InventoryController, interactiveItem: InteractiveItem, gameEntityType: GameEntityType) {
        val gamePlayer = player.toGamePlayerInstance() ?: return
        val isSelected = gamePlayer.selectedEntityType == gameEntityType

        if (isSelected) Blocko.instance.gameEntityHandler.setSelectedEntityType(player.uniqueId, gameEntityType)

        interactiveItem.update(controller, InteractiveItem.Modification.GLOWING, isSelected)
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

        val possibleAchievementPlaceholder: TagResolver.Single = if (gameEntityType.achievementClass == null) Placeholder.parsed("possible_achievement_name", "-/-")
        else Placeholder.parsed("possible_achievement_name", getAchievementByClass(gameEntityType.achievementClass)?.name
            ?: ":=)")

        val loreSuffixPlaceholder: TagResolver.Single = if (isUnlocked)
            Placeholder.parsed("lore_suffix", "")
        else if (gameEntityType.price > statsPlayer.coins)
            Placeholder.parsed("lore_suffix", translation.lineAsString("blocko.inventory.entity_shop.entity_type_item.lore.suffix.not_buyable"))
        else
            Placeholder.parsed("lore_suffix", translation.lineAsString("blocko.inventory.entity_shop.entity_type_item.lore.suffix.buyable"))

        return translation.lore(loreKey,
            Placeholder.parsed("price", NumberUtils.format(gameEntityType.price)),
            possibleAchievementPlaceholder,
            loreSuffixPlaceholder)
    }

}