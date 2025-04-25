package net.spacetivity.blocko.arena.setup

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.BlockoGame
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
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class GameArenaSetupTool(private val holder: Player) {


    val translation = BlockoGame.instance.translationHandler.getSelectedTranslation()
    val itemStack: ItemStack

    var currentMode: ToolMode = getFallbackToolMode()
    var currentTeamName: String? = null
    var fieldIndex: Int = 0

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
        val nextMode: ToolMode = ToolMode.entries.find { it.modeId == if (isNextModeRequested) this.currentMode.modeId.inc() else this.currentMode.modeId.dec() }
            ?: getFallbackToolMode()

        this.currentMode = nextMode

        val tempItemMeta: ItemMeta = this.itemStack.itemMeta
        tempItemMeta.lore(fetchLore(this.translation))

        heldItemStack.itemMeta = tempItemMeta
        this.itemStack.itemMeta = tempItemMeta

        this.holder.translateMessage("blocko.setup.tool.mode_change", Placeholder.parsed("mode", this.currentMode.modeName))
        this.holder.playSound(this.holder.location, Sound.ENTITY_PLAYER_LEVELUP, 1F, 1F)
    }

    fun doAction(event: PlayerInteractEvent) {
        val block: Block = event.clickedBlock ?: return
        if (this.currentMode.validBlockTypes.isNotEmpty() && !this.currentMode.validBlockTypes.contains(block.type.name)) return

        when (this.currentMode) {
            ToolMode.SCAN_BOARD -> {
                BlockoGame.instance.gameArenaSetupHandler.selectCorner(this.holder, event.action.isLeftClick, block.location)
            }

            ToolMode.SET_TURN -> {
                InventoryUtils.openGameFieldTurnInventory(this.holder, block.location)
            }

            ToolMode.SET_TEAM_ENTRANCE -> {
                InventoryUtils.openGameTeamSetupInventory(this.holder, InvType.ENTRANCE, block)
            }

            ToolMode.SET_TEAM_PATH -> {
                if (this.currentTeamName == null || (event.action.isLeftClick && this.currentTeamName != null)) {
                    InventoryUtils.openGameTeamSetupInventory(this.holder, InvType.IDS, block)
                } else {
                    BlockoGame.instance.gameArenaSetupHandler.setFieldId(this.holder, this.currentTeamName!!, block.location)
                }
            }
        }
    }

    private fun fetchLore(translation: Translation): MutableList<Component> {
        val lore: MutableList<Component> = mutableListOf()
        val optionalDataPrefix = translation.displayName("blocko.setup.tool.lore.separator")

        for (toolMode: ToolMode in ToolMode.entries) {
            val active = this.currentMode == toolMode
            val modeId = toolMode.modeId + 1

            val loreModeTitle = translation.displayName("blocko.setup.tool.lore.mode_title.${if (active) "active" else "not_active"}",
                Placeholder.parsed("mode_id", modeId.toString()),
                Placeholder.parsed("mode", toolMode.modeName))

            lore.add(loreModeTitle)

            for (keybindHint in toolMode.toolModeKeybindHints) {
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

    private fun getFallbackToolMode(): ToolMode {
        return ToolMode.entries.sortedBy { it.modeId }.min()
    }

    // SHIFT + Left-Click => Next Mode | SHIFT + Right-Click => Previous Mode
    enum class ToolMode(val modeName: String, val modeId: Int, val toolModeKeybindHints: Set<ToolModeKeybindHint>, val validBlockTypes: List<String>) {
        SCAN_BOARD(
            "Scan Board",
            0,
            setOf(
                ToolModeKeybindHint(ToolModeKeybind.LEFT_CLICK, "Pos1"),
                ToolModeKeybindHint(ToolModeKeybind.RIGHT_CLICK, "Pos2"),
            ),
            listOf()
        ),

        SET_TURN(
            "Set Turning Point",
            1,
            setOf(
                ToolModeKeybindHint(ToolModeKeybind.RIGHT_CLICK, null)
            ),
            listOf("RED_WOOL", "GREEN_WOOL", "BLUE_WOOL", "YELLOW_WOOL", "BONE_BLOCK")
        ),

        SET_TEAM_ENTRANCE(
            "Set Team Entrance",
            2,
            setOf(
                ToolModeKeybindHint(ToolModeKeybind.RIGHT_CLICK, null)
            ),
            listOf("BONE_BLOCK")
        ),

        SET_TEAM_PATH(
            "Set Team Path",
            3,
            setOf(
                ToolModeKeybindHint(ToolModeKeybind.LEFT_CLICK, "Opens Team Selector"),
                ToolModeKeybindHint(ToolModeKeybind.RIGHT_CLICK, "Sets teamId to gameField")
            ),
            listOf("RED_CONCRETE", "GREEN_CONCRETE", "BLUE_CONCRETE", "YELLOW_CONCRETE", "BONE_BLOCK")
        );
    }

    data class ToolModeKeybindHint(val keybind: ToolModeKeybind, val hint: String?)

    enum class ToolModeKeybind(val keybindName: String) {
        LEFT_CLICK("Left-Click"),
        RIGHT_CLICK("Right-Click")
    }

}