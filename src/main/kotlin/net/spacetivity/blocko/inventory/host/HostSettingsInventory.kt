package net.spacetivity.blocko.inventory.host

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.Arena
import net.spacetivity.blocko.item.hideExtraInfo
import net.spacetivity.blocko.item.itemStack
import net.spacetivity.blocko.item.meta
import net.spacetivity.blocko.item.name
import net.spacetivity.blocko.team.GameTeamOptions
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.utils.InventoryUtils
import net.spacetivity.inventory.api.inventory.InventoryController
import net.spacetivity.inventory.api.inventory.InventoryProperties
import net.spacetivity.inventory.api.inventory.InventoryProvider
import net.spacetivity.inventory.api.item.InteractiveItem
import net.spacetivity.inventory.api.item.InventoryPos
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player

@InventoryProperties(id = "host_settings_inv", rows = 1, columns = 9)
class HostSettingsInventory(private val arena: Arena) : InventoryProvider {

    override fun init(player: Player, controller: InventoryController) {
        val translation: Translation = BlockoGame.instance.translationHandler.getSelectedTranslation()

        controller.setItem(0, 1, InteractiveItem.of(itemStack(Material.WRITABLE_BOOK) {
            meta {
                name = translation.displayName("blocko.inventory.host.invite_players.display_name")
                hideExtraInfo()
            }
        }) { _, _, _ -> InventoryUtils.openInvitationInventory(player, this.arena) })

        controller.setItem(0, 2, InteractiveItem.of(itemStack(Material.END_CRYSTAL) {
            meta {
                name = buildTeamModeSelectorDisplayName(translation)
                lore(buildTeamModeSelectorLore(translation))
                hideExtraInfo()
            }
        }) { _, item, event ->
            val nextMode = GameTeamOptions.entries.find { it.id == if (event.isLeftClick) this.arena.teamOptions.id.inc() else this.arena.teamOptions.id.dec() } ?: GameTeamOptions.FOUR_BY_ONE

            this.arena.teamOptions = nextMode

            BlockoGame.instance.arenaSignHandler.updateArenaSign(this.arena)

            item.update(controller, InteractiveItem.Modification.DISPLAY_NAME, buildTeamModeSelectorDisplayName(translation))
            item.update(controller, InteractiveItem.Modification.LORE, buildTeamModeSelectorLore(translation))

            player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1F, 1F)
        })

        setIndicatorItem(controller, InventoryPos.of(0, 6), IndicatorType.PRIVACY, translation, player)
        setIndicatorItem(controller, InventoryPos.of(0, 7), IndicatorType.WAITING_PREDICATE, translation, player)
    }

    private fun setIndicatorItem(controller: InventoryController, position: InventoryPos, indicatorType: IndicatorType, translation: Translation, player: Player) {
        controller.setItem(position, InteractiveItem.of(itemStack(getIndicatorMaterialType(indicatorType)) {
            meta {
                name = getIndicatorDisplayName(indicatorType, translation)
                hideExtraInfo()
            }
        }) { _, item, _ ->
            if (indicatorType == IndicatorType.PRIVACY) this.arena.locked = !this.arena.locked
            else this.arena.waitForActualPlayers = !this.arena.waitForActualPlayers

            if (indicatorType == IndicatorType.WAITING_PREDICATE) {
                if (this.arena.waitForActualPlayers) arena.phase.countdown?.cancel()
                else this.arena.phase.countdown?.tryStartup()
            }

            item.update(controller, InteractiveItem.Modification.TYPE, getIndicatorMaterialType(indicatorType))
            item.update(controller, InteractiveItem.Modification.DISPLAY_NAME, getIndicatorDisplayName(indicatorType, translation))

            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F)
        })
    }

    private fun buildTeamModeSelectorDisplayName(translation: Translation): Component {
        return translation.displayName("blocko.inventory.host.team_mode_changer.display_name", Placeholder.parsed("mode", this.arena.teamOptions.getDisplayString()))
    }

    private fun buildTeamModeSelectorLore(translation: Translation): MutableList<Component> {
        val lore = mutableListOf<Component>()

        for (teamOptions in GameTeamOptions.entries) {
            val color = if (this.arena.teamOptions == teamOptions) NamedTextColor.GREEN else NamedTextColor.DARK_GRAY

            lore.add(translation.displayName("blocko.inventory.host.team_mode_changer.lore_line_format",
                Placeholder.parsed("mode_color", "<${color.asHexString()}>"),
                Placeholder.parsed("mode", teamOptions.getDisplayString())))
        }

        return lore
    }

    private fun getIndicatorMaterialType(indicatorType: IndicatorType): Material = when (indicatorType) {
        IndicatorType.PRIVACY -> if (this.arena.locked) Material.BARRIER else Material.OAK_DOOR
        IndicatorType.WAITING_PREDICATE -> if (this.arena.waitForActualPlayers) Material.POTION else Material.GLASS_BOTTLE
    }

    private fun getIndicatorDisplayName(indicatorType: IndicatorType, translation: Translation): Component =
        when (indicatorType) {
            IndicatorType.PRIVACY -> {
                val placeholder: TagResolver.Single = Placeholder.parsed("status", translation.lineAsString("blocko.inventory.host.arena_status.${if (this.arena.locked) "active" else "not_active"}"))
                translation.displayName("blocko.inventory.host.arena_status.display_name", placeholder)
            }

            IndicatorType.WAITING_PREDICATE -> {
                val placeholder: TagResolver.Single = Placeholder.parsed("status", translation.lineAsString("blocko.inventory.host.wait_for_players.${if (this.arena.waitForActualPlayers) "active" else "not_active"}"))
                translation.displayName("blocko.inventory.host.wait_for_players.display_name", placeholder)
            }
        }

    private enum class IndicatorType {
        PRIVACY,
        WAITING_PREDICATE
    }

}