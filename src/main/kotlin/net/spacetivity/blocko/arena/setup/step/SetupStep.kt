package net.spacetivity.blocko.arena.setup.step

import net.spacetivity.blocko.arena.setup.SetupTool.ToolModeKeybindHint
import org.bukkit.Material
import org.bukkit.entity.Player

interface SetupStep {
    val id: Int
    val name: String
    val keybindHints: Set<ToolModeKeybindHint>
    val validBlockTypes: Set<Material>
    var active: Boolean

    fun reset(player: Player)

}