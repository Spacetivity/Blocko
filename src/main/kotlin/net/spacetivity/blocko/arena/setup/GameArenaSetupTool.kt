package net.spacetivity.blocko.arena.setup

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.inventory.setup.GameTeamSetupInventory
import net.spacetivity.blocko.inventory.setup.InvType
import net.spacetivity.blocko.utils.InventoryUtils
import net.spacetivity.blocko.utils.ItemBuilder
import net.spacetivity.blocko.utils.PersistentDataUtils
import net.spacetivity.inventory.api.extension.openStaticInventory
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class GameArenaSetupTool(private val holder: Player) {

    var currentMode: ToolMode = ToolMode.ADD_FIELD

    val itemStack: ItemStack = ItemBuilder(Material.entries.find { it.name == BlockoGame.instance.globalConfigFile.setupItemType }
        ?: throw NullPointerException("Invalid setup item type!"))
        .setName(Component.text("Setup Tool", NamedTextColor.AQUA, TextDecoration.BOLD))
        .setLoreByComponent(fetchLore())
        .build()

    var currentTeamName: String? = null
    var fieldIndex: Int = 0

    fun setToPlayer() {
        PersistentDataUtils.setData(this.itemStack, this.itemStack.itemMeta, "setupTool", this.holder.uniqueId.toString())
        this.holder.inventory.addItem(this.itemStack)
    }

    fun onToggle(increase: Boolean, heldItemStack: ItemStack) {
        val nextMode: ToolMode = ToolMode.entries.find { it.modeId == if (increase) this.currentMode.modeId.inc() else this.currentMode.modeId.dec() }
            ?: ToolMode.ADD_FIELD

        this.currentMode = nextMode

        val tempItemMeta: ItemMeta = this.itemStack.itemMeta
        tempItemMeta.lore(fetchLore())

        heldItemStack.itemMeta = tempItemMeta
        this.itemStack.itemMeta = tempItemMeta
        this.holder.playSound(this.holder.location, Sound.ENTITY_PLAYER_LEVELUP, 1F, 1F)
    }

    fun doAction(event: PlayerInteractEvent) {
        val block: Block = event.clickedBlock ?: return
        if (!this.currentMode.validBlockTypes.contains(block.type.name)) return

        when (this.currentMode) {
            ToolMode.ADD_FIELD -> BlockoGame.instance.gameArenaSetupHandler.addField(this.holder, block.location)
            ToolMode.SET_TURN -> InventoryUtils.openGameFieldTurnInventory(this.holder, block.location)
            ToolMode.SET_GARAGE_FIELD -> InventoryUtils.openGameTeamSetupInventory(this.holder, InvType.GARAGE, block)
            ToolMode.SET_TEAM_ENTRANCE -> InventoryUtils.openGameTeamSetupInventory(this.holder, InvType.ENTRANCE, block)
            ToolMode.SET_TEAM_PATH -> {
                if (this.currentTeamName == null || (this.holder.isSneaking && this.currentTeamName != null)) openStaticInventory(this.holder, Component.text("Set team path"), GameTeamSetupInventory(InvType.IDS, block.location))
                else BlockoGame.instance.gameArenaSetupHandler.setFieldId(this.holder, this.currentTeamName!!, block.location)
            }
        }
    }

    private fun fetchLore(): MutableList<Component> {
        val lore: MutableList<Component> = mutableListOf()

        for (toolMode: ToolMode in ToolMode.entries) {
            lore.add(fetchLine(toolMode))
        }

        return lore
    }

    private fun fetchLine(toolMode: ToolMode): Component {
        val color: NamedTextColor = if (this.currentMode == toolMode) NamedTextColor.GREEN else NamedTextColor.DARK_GRAY
        return Component.text(toolMode.modeName, color)
    }

    enum class ToolMode(val modeName: String, val modeId: Int, val validBlockTypes: List<String>) {
        ADD_FIELD("Add Field", 0, listOf("RED_WOOL", "GREEN_WOOL", "BLUE_WOOL", "YELLOW_WOOL", "BONE_BLOCK")),
        SET_TURN("Set Turn", 1, listOf("RED_WOOL", "GREEN_WOOL", "BLUE_WOOL", "YELLOW_WOOL", "BONE_BLOCK")),
        SET_GARAGE_FIELD("Set Garage Field", 2, listOf("RED_WOOL", "GREEN_WOOL", "BLUE_WOOL", "YELLOW_WOOL")),
        SET_TEAM_ENTRANCE("Set Team Entrance", 3, listOf("BONE_BLOCK")),
        SET_TEAM_PATH("Set Team Path", 4, listOf("RED_WOOL", "GREEN_WOOL", "BLUE_WOOL", "YELLOW_WOOL", "BONE_BLOCK"));
    }

}