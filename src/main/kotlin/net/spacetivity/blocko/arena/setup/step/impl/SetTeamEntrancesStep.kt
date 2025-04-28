package net.spacetivity.blocko.arena.setup.step.impl

import net.spacetivity.blocko.arena.setup.GameArenaSetupTool.ToolModeKeybind
import net.spacetivity.blocko.arena.setup.GameArenaSetupTool.ToolModeKeybindHint
import net.spacetivity.blocko.arena.setup.getSetupSession
import net.spacetivity.blocko.arena.setup.step.SetupStep
import org.bukkit.Material
import org.bukkit.entity.Player

class SetTeamEntrancesStep : SetupStep {

    override val id: Int = 2
    override val name: String = "Set Team Entrance"
    override val keybindHints: Set<ToolModeKeybindHint> = setOf(ToolModeKeybindHint(ToolModeKeybind.RIGHT_CLICK, null))
    override val validBlockTypes: Set<Material> = setOf(Material.BONE_BLOCK)
    override var active: Boolean = false

    override fun reset(player: Player) {
        val setupSession = player.getSetupSession() ?: return
        val scanBoardStep = setupSession.getSetupStep(ScanBoardStep::class) ?: return

        for (gameField in scanBoardStep.gameFields) {
            if (gameField.properties.teamEntrance == null) continue
            gameField.properties.teamEntrance = null
        }
    }

}