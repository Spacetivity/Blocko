package net.spacetivity.blocko.inventory.host

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.team.GameTeamOptions
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.utils.InventoryUtils
import net.spacetivity.blocko.utils.ItemBuilder
import net.spacetivity.inventory.api.inventory.InventoryController
import net.spacetivity.inventory.api.inventory.InventoryProperties
import net.spacetivity.inventory.api.inventory.InventoryProvider
import net.spacetivity.inventory.api.item.InteractiveItem
import net.spacetivity.inventory.api.item.InventoryPos
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemFlag

@InventoryProperties(id = "host_settings_inv", rows = 1, columns = 9)
class HostSettingsInventory(private val gameArena: GameArena) : InventoryProvider {

    override fun init(player: Player, controller: InventoryController) {
        val translation: Translation = BlockoGame.instance.translationHandler.getSelectedTranslation()

        controller.setItem(0, 1, InteractiveItem.of(ItemBuilder(Material.WRITABLE_BOOK)
            .setName(translation.validateItemName("blocko.inventory.host.invite_players.display_name"))
            .addFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
            .build()) { _, _, _ -> InventoryUtils.openInvitationInventory(player, this.gameArena) })

        controller.setItem(0, 2, InteractiveItem.of(ItemBuilder(Material.END_CRYSTAL)
            .setName(buildTeamModeSelectorDisplayName(translation))
            .setLoreByComponent(buildTeamModeSelectorLore(translation))
            .addFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
            .build()) { _, item: InteractiveItem, event: InventoryClickEvent ->

            val nextMode: GameTeamOptions = GameTeamOptions.entries.find { it.id == if (event.isLeftClick) this.gameArena.teamOptions.id.inc() else this.gameArena.teamOptions.id.dec() }
                ?: GameTeamOptions.FOUR_BY_ONE

            this.gameArena.teamOptions = nextMode

            BlockoGame.instance.gameArenaSignHandler.updateArenaSign(this.gameArena)

            item.update(controller, InteractiveItem.Modification.DISPLAY_NAME, buildTeamModeSelectorDisplayName(translation))
            item.update(controller, InteractiveItem.Modification.LORE, buildTeamModeSelectorLore(translation))

            player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1F, 1F)
        })

        setIndicatorItem(controller, InventoryPos.of(0, 6), IndicatorType.PRIVACY, translation, player)
        setIndicatorItem(controller, InventoryPos.of(0, 7), IndicatorType.WAITING_PREDICATE, translation, player)
    }

    private fun setIndicatorItem(controller: InventoryController, position: InventoryPos, indicatorType: IndicatorType, translation: Translation, player: Player) {
        controller.setItem(position, InteractiveItem.of(ItemBuilder(getIndicatorMaterialType(indicatorType))
            .setName(getIndicatorDisplayName(indicatorType, translation))
            .addFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
            .build()) { _, item: InteractiveItem, _ ->

            if (indicatorType == IndicatorType.PRIVACY) this.gameArena.locked = !this.gameArena.locked
            else this.gameArena.waitForActualPlayers = !this.gameArena.waitForActualPlayers

            if (indicatorType == IndicatorType.WAITING_PREDICATE) {
                if (this.gameArena.waitForActualPlayers) gameArena.phase.countdown?.cancel()
                else this.gameArena.phase.countdown?.tryStartup()
            }

            item.update(controller, InteractiveItem.Modification.TYPE, getIndicatorMaterialType(indicatorType))
            item.update(controller, InteractiveItem.Modification.DISPLAY_NAME, getIndicatorDisplayName(indicatorType, translation))

            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F)
        })
    }

    private fun buildTeamModeSelectorDisplayName(translation: Translation): Component {
        return translation.validateItemName("blocko.inventory.host.team_mode_changer.display_name", Placeholder.parsed("mode", this.gameArena.teamOptions.getDisplayString()))
    }

    private fun buildTeamModeSelectorLore(translation: Translation): MutableList<Component> {
        val lore: MutableList<Component> = mutableListOf()

        for (teamOptions: GameTeamOptions in GameTeamOptions.entries) {
            val color: NamedTextColor = if (this.gameArena.teamOptions == teamOptions) NamedTextColor.GREEN else NamedTextColor.DARK_GRAY

            lore.add(translation.validateItemName("blocko.inventory.host.team_mode_changer.lore_line_format",
                Placeholder.parsed("mode_color", "<${color.asHexString()}>"),
                Placeholder.parsed("mode", teamOptions.getDisplayString())))
        }

        return lore
    }

    private fun getIndicatorMaterialType(indicatorType: IndicatorType): Material = when (indicatorType) {
        IndicatorType.PRIVACY -> if (this.gameArena.locked) Material.BARRIER else Material.OAK_DOOR
        IndicatorType.WAITING_PREDICATE -> if (this.gameArena.waitForActualPlayers) Material.POTION else Material.GLASS_BOTTLE
    }

    private fun getIndicatorDisplayName(indicatorType: IndicatorType, translation: Translation): Component =
        when (indicatorType) {
            IndicatorType.PRIVACY -> {
                val placeholder: TagResolver.Single = Placeholder.parsed("status", translation.validateLineAsString("blocko.inventory.host.arena_status.${if (this.gameArena.locked) "active" else "not_active"}"))
                translation.validateItemName("blocko.inventory.host.arena_status.display_name", placeholder)
            }

            IndicatorType.WAITING_PREDICATE -> {
                val placeholder: TagResolver.Single = Placeholder.parsed("status", translation.validateLineAsString("blocko.inventory.host.wait_for_players.${if (this.gameArena.waitForActualPlayers) "active" else "not_active"}"))
                translation.validateItemName("blocko.inventory.host.wait_for_players.display_name", placeholder)
            }
        }

    private enum class IndicatorType {
        PRIVACY,
        WAITING_PREDICATE
    }

}