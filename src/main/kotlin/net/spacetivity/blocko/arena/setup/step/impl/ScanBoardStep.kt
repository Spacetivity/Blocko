package net.spacetivity.blocko.arena.setup.step.impl

import net.spacetivity.blocko.arena.setup.GameArenaSetupTool.ToolModeKeybind
import net.spacetivity.blocko.arena.setup.GameArenaSetupTool.ToolModeKeybindHint
import net.spacetivity.blocko.arena.setup.ScannerResult
import net.spacetivity.blocko.arena.setup.step.SetupStep
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.team.GameTeamLocation
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player

class ScanBoardStep : SetupStep {

    override val id: Int = 0
    override val name: String = "Scan Board"
    override val keybindHints: Set<ToolModeKeybindHint> = setOf(ToolModeKeybindHint(ToolModeKeybind.LEFT_CLICK, "Pos1"), ToolModeKeybindHint(ToolModeKeybind.RIGHT_CLICK, "Pos2"))
    override val validBlockTypes: Set<Material> = setOf()
    override var active: Boolean = true

    var fieldIndex: Int = 0
    val gameFields: MutableList<GameField> = mutableListOf()
    val gameTeamLocations: MutableList<GameTeamLocation> = mutableListOf()

    var corner1: Location? = null
    var corner2: Location? = null
    var missingResults: MutableMap<ScannerResult, Int> = mutableMapOf()

    fun areCornersSet(): Boolean = this.corner1 != null && this.corner2 != null

    override fun reset(player: Player) {
        this.gameFields.clear()
        this.gameTeamLocations.clear()
        this.corner1 = null
        this.corner2 = null
        this.missingResults.clear()
    }

}