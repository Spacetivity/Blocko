package net.spacetivity.blocko.setup.step

import net.kyori.adventure.text.Component
import net.spacetivity.blocko.setup.SetupTool
import net.spacetivity.blocko.setup.tooltips.Tooltip
import org.bukkit.Material
import org.bukkit.entity.Player

interface SetupStep<R : ResetData> {
    val id: Int
    val key: String
    val keybindHints: Set<SetupTool.ToolModeKeybindHint>
    val validBlockTypes: Set<Material>
    val tooltip: Tooltip
    var active: Boolean

    fun getSidebarLines(player: Player): List<Component>

    fun reset(player: Player, optionalData: R?)

}