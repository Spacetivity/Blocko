package net.spacetivity.blocko.arena.setup.step.impl

import net.spacetivity.blocko.arena.setup.SetupTool.ToolModeKeybind
import net.spacetivity.blocko.arena.setup.SetupTool.ToolModeKeybindHint
import net.spacetivity.blocko.arena.setup.getSetupSession
import net.spacetivity.blocko.arena.setup.step.SetupStep
import org.bukkit.Material
import org.bukkit.entity.Player

class SetTurningPointsStep : SetupStep {

    override val id = 1
    override val name = "Set Turning Point"

    override val keybindHints = setOf(
        ToolModeKeybindHint(ToolModeKeybind.RIGHT_CLICK, null)
    )

    override val validBlockTypes = setOf(
        Material.RED_WOOL,
        Material.GREEN_WOOL,
        Material.BLUE_WOOL,
        Material.YELLOW_WOOL,
        Material.BONE_BLOCK
    )

    override var active = false

    override fun reset(player: Player) {
        val setupSession = player.getSetupSession() ?: return
        val scanBoardStep = setupSession.getSetupStep(ScanBoardStep::class) ?: return

        for (gameField in scanBoardStep.gameFields) {
            if (gameField.properties.rotation == null) continue
            gameField.properties.rotation = null
        }
    }

}