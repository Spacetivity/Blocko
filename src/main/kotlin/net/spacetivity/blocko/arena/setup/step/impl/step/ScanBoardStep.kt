package net.spacetivity.blocko.arena.setup.step.impl.step

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.arena.setup.ScannerResult
import net.spacetivity.blocko.arena.setup.SetupTool.ToolModeKeybind
import net.spacetivity.blocko.arena.setup.SetupTool.ToolModeKeybindHint
import net.spacetivity.blocko.arena.setup.step.SetupStep
import net.spacetivity.blocko.arena.setup.step.impl.reset.ScanBoardResetData
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.field.highlighting.impl.*
import net.spacetivity.blocko.team.GameTeamLocation
import net.spacetivity.blocko.translation.translateMessage
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player

class ScanBoardStep : SetupStep<ScanBoardResetData> {

    override val id = 0
    override val key = "ScanBoard"
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
    fun isFieldAt(x: Int, z: Int): Boolean = this.gameFields.any { it.x == x && it.z == z }
    fun getField(x: Int, z: Int): GameField? = this.gameFields.find { it.x == x && it.z == z }

    override fun getSidebarLines(player: Player): List<Component> {
        return emptyList()
    }

    override fun reset(player: Player, optionalData: ScanBoardResetData?) {
        if (optionalData == null) return

        this.gameFields.clear()
        this.gameTeamLocations.clear()
        this.corner1 = null
        this.corner2 = null
        this.missingResults.clear()

        Blocko.instance.gameFieldHighlightHandler.removeHighlightEntities(
            optionalData.arenaId,
            GameFieldHighlightMode::class,
            TeamSpawnHighlightMode::class,
            TeamPathHighlightMode::class,
            GarageFieldHighlightMode::class,
            TurningPointHighlightMode::class
        )

        player.translateMessage("blocko.setup.reset.step.success.generic", Placeholder.parsed("step", this.key))
    }

}