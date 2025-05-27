package net.spacetivity.blocko.setup.step.impl.step

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.field.highlighting.impl.GameFieldHighlightMode
import net.spacetivity.blocko.setup.SetupTool
import net.spacetivity.blocko.setup.getSetupSession
import net.spacetivity.blocko.setup.step.SetupStep
import net.spacetivity.blocko.setup.step.impl.reset.IgnoredResetData
import net.spacetivity.blocko.setup.tooltips.Tooltip
import net.spacetivity.blocko.translation.translateMessage
import org.bukkit.Material
import org.bukkit.entity.Player

class SetTurningPointsStep : SetupStep<IgnoredResetData> {

    override val id = 1
    override val key = "SetTurningPoint"

    override val keybindHints = setOf(
        SetupTool.ToolModeKeybindHint(SetupTool.ToolModeKeybind.RIGHT_CLICK, null)
    )

    override val validBlockTypes = setOf(
        Material.RED_WOOL,
        Material.GREEN_WOOL,
        Material.BLUE_WOOL,
        Material.YELLOW_WOOL,
        Material.BONE_BLOCK
    )

    override val tooltip = Tooltip(this.id)
    override var active = false

    override fun getSidebarLines(player: Player): List<Component> {
        return emptyList()
    }

    override fun reset(player: Player, optionalData: IgnoredResetData?) {
        val setupSession = player.getSetupSession() ?: return
        val scanBoardStep = setupSession.getSetupStep<ScanBoardStep>() ?: return

        for (gameField in scanBoardStep.gameFields) {
            if (gameField.properties.rotation == null) continue
            gameField.properties.rotation = null

            Blocko.instance.gameFieldHighlightHandler.spawnOrUpdateHighlightEntity(gameField.arenaId, gameField.getWorldPosition(true), GameFieldHighlightMode::class)
        }

        player.translateMessage("blocko.setup.reset.step.success.generic", Placeholder.parsed("step", this.key))
    }

}