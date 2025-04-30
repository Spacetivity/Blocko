package net.spacetivity.blocko.arena.setup

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.setup.step.impl.ScanBoardStep
import net.spacetivity.blocko.arena.setup.step.impl.SetTeamEntrancesStep
import net.spacetivity.blocko.arena.setup.step.impl.SetTeamPathsStep
import net.spacetivity.blocko.arena.setup.step.impl.SetTurningPointsStep
import net.spacetivity.blocko.inventory.setup.InvType
import net.spacetivity.blocko.item.hideExtraInfo
import net.spacetivity.blocko.item.itemStack
import net.spacetivity.blocko.item.meta
import net.spacetivity.blocko.item.name
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.translation.translateMessage
import net.spacetivity.blocko.utils.Constants
import net.spacetivity.blocko.utils.InventoryUtils
import net.spacetivity.blocko.utils.PersistentDataUtils
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

class SetupTool(private val holder: Player) {

    val translation = BlockoGame.instance.translationHandler.getSelectedTranslation()
    val itemStack: ItemStack

    init {
        val type = Material.entries.find { it.name == BlockoGame.instance.globalConfigFile.setupItemType }
            ?: throw NullPointerException("Invalid setup item type!")

        this.itemStack = itemStack(type) {
            meta {
                name = translation.displayName("blocko.setup.tool.display_name")
                lore(fetchLore(translation))
                hideExtraInfo()
            }
        }
    }

    fun setToPlayer() {
        val meta = itemStack.itemMeta
        PersistentDataUtils.apply(meta, Constants.SETUP_TOOL_KEY, this.holder.uniqueId.toString())
        this.itemStack.itemMeta = meta
        this.holder.inventory.addItem(this.itemStack)
    }

    fun onToggle(isNextModeRequested: Boolean, heldItemStack: ItemStack) {
        val setupSession = this.holder.getSetupSession() ?: return
        val nextSetupStep = setupSession.setNextSetupStep(isNextModeRequested)

        val tempItemMeta = this.itemStack.itemMeta
        tempItemMeta.lore(fetchLore(this.translation))

        heldItemStack.itemMeta = tempItemMeta
        this.itemStack.itemMeta = tempItemMeta

        this.holder.translateMessage("blocko.setup.tool.mode_change", Placeholder.parsed("mode", nextSetupStep.name))
        this.holder.playSound(this.holder.location, Sound.ENTITY_PLAYER_LEVELUP, 1F, 1F)
    }

    fun doAction(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return
        val setupSession = this.holder.getSetupSession() ?: return
        val activeStep = setupSession.getActiveSetupStep() ?: return

        if (activeStep.validBlockTypes.isNotEmpty() && !activeStep.validBlockTypes.contains(block.type)) return

        when (activeStep::class) {
            ScanBoardStep::class -> {
                BlockoGame.instance.arenaSetupHandler.selectCorner(this.holder, event.action.isLeftClick, block.location)
            }

            SetTurningPointsStep::class -> {
                InventoryUtils.openGameFieldTurnInventory(this.holder, block.location)
            }

            SetTeamEntrancesStep::class -> {
                InventoryUtils.openGameTeamSetupInventory(this.holder, InvType.ENTRANCE, block)
            }

            SetTeamPathsStep::class -> {
                if (setupSession.currentTeamName == null || (event.action.isLeftClick && setupSession.currentTeamName != null)) {
                    InventoryUtils.openGameTeamSetupInventory(this.holder, InvType.IDS, block)
                } else {
                    BlockoGame.instance.arenaSetupHandler.setFieldTeamId(this.holder, setupSession.currentTeamName!!, block.location)
                }
            }
        }
    }

    private fun fetchLore(translation: Translation): MutableList<Component> {
        val lore = mutableListOf<Component>()
        val optionalDataPrefix = translation.displayName("blocko.setup.tool.lore.separator")

        val setupSession = this.holder.getSetupSession() ?: return lore

        for (setupStep in setupSession.setupSteps.values) {
            val stepId = setupStep.id + 1 // +1 because it looks nicer for players ingame to think of the first setup step as 1

            val loreModeTitle = translation.displayName("blocko.setup.tool.lore.mode_title.${if (setupStep.active) "active" else "not_active"}",
                Placeholder.parsed("mode_id", stepId.toString()),
                Placeholder.parsed("mode", setupStep.name))

            lore.add(loreModeTitle)

            for (keybindHint in setupStep.keybindHints) {
                val keybind = keybindHint.keybind
                val hint = keybindHint.hint

                val placeholders: MutableList<TagResolver> = mutableListOf(Placeholder.parsed("keybind", keybind.keybindName))

                placeholders.add(Placeholder.component("data_prefix", if (hint == null) Component.text("") else optionalDataPrefix))
                placeholders.add(Placeholder.parsed("optional_data", hint ?: ""))

                val loreModeKeybindLine = translation.displayName("blocko.setup.tool.lore.mode_keybind_line", *placeholders.toTypedArray())
                lore.add(loreModeKeybindLine)
            }
        }

        return lore
    }

    data class ToolModeKeybindHint(val keybind: ToolModeKeybind, val hint: String?)

    enum class ToolModeKeybind(val keybindName: String) {
        LEFT_CLICK("Left-Click"),
        RIGHT_CLICK("Right-Click")
    }

}