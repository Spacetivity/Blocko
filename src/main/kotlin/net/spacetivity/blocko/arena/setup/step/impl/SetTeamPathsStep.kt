package net.spacetivity.blocko.arena.setup.step.impl

import net.spacetivity.blocko.arena.setup.SetupTool.ToolModeKeybind
import net.spacetivity.blocko.arena.setup.SetupTool.ToolModeKeybindHint
import net.spacetivity.blocko.arena.setup.getSetupSession
import net.spacetivity.blocko.arena.setup.step.SetupStep
import org.bukkit.Material
import org.bukkit.entity.Player

class SetTeamPathsStep : SetupStep {

    override val id = 3
    override val name = "Set Team Path"

    override val keybindHints = setOf(
        ToolModeKeybindHint(ToolModeKeybind.LEFT_CLICK, "Opens Team Selector"),
        ToolModeKeybindHint(ToolModeKeybind.RIGHT_CLICK, "Sets teamId to gameField")
    )

    override val validBlockTypes = setOf(
        Material.RED_CONCRETE,
        Material.GREEN_CONCRETE,
        Material.BLUE_CONCRETE,
        Material.YELLOW_CONCRETE,

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
            gameField.properties.teamFieldIds.clear()
        }
    }

}