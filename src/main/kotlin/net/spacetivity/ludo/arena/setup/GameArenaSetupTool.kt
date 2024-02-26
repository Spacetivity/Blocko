package net.spacetivity.ludo.arena.setup

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.spacetivity.inventory.api.extension.openStaticInventory
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.inventory.GameFieldTurnSetupInventory
import net.spacetivity.ludo.inventory.GameTeamSetupInventory
import net.spacetivity.ludo.inventory.InvType
import net.spacetivity.ludo.utils.ItemBuilder
import net.spacetivity.ludo.utils.PersistentDataUtils
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

    val itemStack: ItemStack = ItemBuilder(Material.entries.find { it.name == LudoGame.instance.globalConfigFile.setupItemType }
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
            ToolMode.ADD_FIELD -> LudoGame.instance.gameArenaSetupHandler.addField(this.holder, block.location)
            ToolMode.SET_TURN -> Bukkit.getServer().openStaticInventory(this.holder, Component.text("Set a turn"), GameFieldTurnSetupInventory(block.location))
            ToolMode.SET_GARAGE_FIELD -> Bukkit.getServer().openStaticInventory(this.holder, Component.text("Add garage field"), GameTeamSetupInventory(InvType.GARAGE, block.location))
            ToolMode.SET_TEAM_ENTRANCE -> Bukkit.getServer().openStaticInventory(this.holder, Component.text("Set team entrance"), GameTeamSetupInventory(InvType.ENTRANCE, block.location))
            ToolMode.SET_TEAM_PATH -> {
                if (this.currentTeamName == null || (this.holder.isSneaking && this.currentTeamName != null)) Bukkit.getServer().openStaticInventory(this.holder, Component.text("Set team path"), GameTeamSetupInventory(InvType.IDS, block.location))
                else LudoGame.instance.gameArenaSetupHandler.setFieldId(this.holder, this.currentTeamName!!, block.location)
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