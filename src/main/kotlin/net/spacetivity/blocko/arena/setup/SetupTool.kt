package net.spacetivity.blocko.arena.setup

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.arena.setup.step.impl.step.ScanBoardStep
import net.spacetivity.blocko.arena.setup.step.impl.step.SetTeamEntrancesStep
import net.spacetivity.blocko.arena.setup.step.impl.step.SetTeamPathsStep
import net.spacetivity.blocko.arena.setup.step.impl.step.SetTurningPointsStep
import net.spacetivity.blocko.inventory.setup.InvType
import net.spacetivity.blocko.item.*
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.translation.translateMessage
import net.spacetivity.blocko.utils.Constants
import net.spacetivity.blocko.utils.InventoryUtils
import net.spacetivity.blocko.utils.ScoreboardUtils
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

class SetupTool(private val holder: Player) {

    private val tooltipHandler = Blocko.instance.tooltipHandler
    private val translation = Blocko.instance.translationHandler.getSelectedTranslation()

    private val type = Material.entries.find { it.name == Blocko.instance.globalConfigFile.setupItemType }
        ?: throw NullPointerException("Invalid setup item type!")

    fun setToPlayer() {
        val itemStack = itemStack(type) {
            meta {
                name = translation.displayName("blocko.setup.tool.display_name")
                lore(fetchLore(translation))
                hideExtraInfo()
                applyPersistentData(Constants.SETUP_TOOL_KEY, holder.uniqueId.toString())
            }
        }

        this.holder.inventory.addItem(itemStack)
    }

    fun onToggle(isNextModeRequested: Boolean, itemStack: ItemStack) {
        val setupSession = this.holder.getSetupSession() ?: return
        val nextSetupStep = setupSession.setNextSetupStep(isNextModeRequested)

        ScoreboardUtils.updateSetupSidebarTitle(this.holder)
        ScoreboardUtils.updateSetupDataLines(this.holder, nextSetupStep)

        itemStack.meta {
            lore(fetchLore(translation))
        }

        if (!this.tooltipHandler.hasViewedTooltip(this.holder.uniqueId, nextSetupStep.id)) {
            val tooltip = this.tooltipHandler.getTooltip(nextSetupStep) ?: return
            this.tooltipHandler.addViewedTooltip(this.holder.uniqueId, nextSetupStep.id)
            this.holder.sendMessage(tooltip.getToolTipComponent(this.translation))
        }

        this.holder.translateMessage("blocko.setup.tool.mode_change", Placeholder.parsed("mode", nextSetupStep.key))
        this.holder.playSound(this.holder.location, Sound.ENTITY_PLAYER_LEVELUP, 1F, 1F)
    }

    fun doAction(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return
        val setupSession = this.holder.getSetupSession() ?: return
        val activeStep = setupSession.getActiveSetupStep() ?: return

        if (activeStep.validBlockTypes.isNotEmpty() && !activeStep.validBlockTypes.contains(block.type)) return

        when (activeStep::class) {
            ScanBoardStep::class -> {
                Blocko.instance.arenaSetupHandler.selectCorner(this.holder, event.action.isLeftClick, block.location)
            }

            SetTurningPointsStep::class -> {
                val scanBoardStep = setupSession.getSetupStep<ScanBoardStep>() ?: return
                val gameField = scanBoardStep.getField(block.x, block.z)
                InventoryUtils.openGameFieldTurnInventory(this.holder, gameField)
            }

            SetTeamEntrancesStep::class -> {
                val scanBoardStep = setupSession.getSetupStep<ScanBoardStep>() ?: return
                val gameField = scanBoardStep.getField(block.x, block.z)
                InventoryUtils.openGameTeamSetupInventory(this.holder, InvType.ENTRANCE, gameField)
            }

            SetTeamPathsStep::class -> {
                if (setupSession.currentTeamName == null || (event.action.isLeftClick && setupSession.currentTeamName != null)) {
                    val scanBoardStep = setupSession.getSetupStep<ScanBoardStep>() ?: return
                    val gameField = scanBoardStep.getField(block.x, block.z)
                    InventoryUtils.openGameTeamSetupInventory(this.holder, InvType.IDS, gameField)
                } else {
                    Blocko.instance.arenaSetupHandler.setTeamPathId(this.holder, setupSession.currentTeamName!!, block)
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
                Placeholder.parsed("mode", setupStep.key))

            lore.add(loreModeTitle)

            for (keybindHint in setupStep.keybindHints) {
                val keybind = keybindHint.keybind
                val hint = keybindHint.hint

                val placeholders = mutableListOf(Placeholder.parsed("keybind", keybind.keybindName))

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