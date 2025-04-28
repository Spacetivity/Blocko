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

    override val id = 0
    override val name = "Scan Board"
    override val keybindHints = setOf(ToolModeKeybindHint(ToolModeKeybind.LEFT_CLICK, "Pos1"), ToolModeKeybindHint(ToolModeKeybind.RIGHT_CLICK, "Pos2"))
    override val validBlockTypes = setOf<Material>()
    override var active = true

    var fieldIndex = 0
    val gameFields = mutableListOf<GameField>()
    val gameTeamLocations = mutableListOf<GameTeamLocation>()

    var corner1: Location? = null
    var corner2: Location? = null
    var missingResults = mutableMapOf<ScannerResult, Int>()

    fun areCornersSet(): Boolean = this.corner1 != null && this.corner2 != null

    override fun reset(player: Player) {
        this.gameFields.clear()
        this.gameTeamLocations.clear()
        this.corner1 = null
        this.corner2 = null
        this.missingResults.clear()
    }

}