package net.spacetivity.blocko.arena.setup.step.impl.step

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.arena.setup.SetupTool.ToolModeKeybind
import net.spacetivity.blocko.arena.setup.SetupTool.ToolModeKeybindHint
import net.spacetivity.blocko.arena.setup.getSetupSession
import net.spacetivity.blocko.arena.setup.step.SetupStep
import net.spacetivity.blocko.arena.setup.step.impl.reset.IgnoredResetData
import net.spacetivity.blocko.field.highlighting.impl.GameFieldHighlightMode
import net.spacetivity.blocko.translation.translateMessage
import org.bukkit.Material
import org.bukkit.entity.Player

class SetTeamEntrancesStep : SetupStep<IgnoredResetData> {

    override val id = 2
    override val key = "SetTeamEntrance"
    override val keybindHints = setOf(ToolModeKeybindHint(ToolModeKeybind.RIGHT_CLICK, null))
    override val validBlockTypes = setOf(Material.BONE_BLOCK)
    override var active = false

    override fun reset(player: Player, optionalData: IgnoredResetData?) {
        val setupSession = player.getSetupSession() ?: return
        val scanBoardStep = setupSession.getSetupStep<ScanBoardStep>() ?: return

        for (gameField in scanBoardStep.gameFields) {
            if (gameField.properties.teamEntrance == null) continue
            gameField.properties.teamEntrance = null
            gameField.properties.rotation = null

            Blocko.instance.gameFieldHighlightHandler.spawnOrUpdateHighlightEntity(gameField.arenaId, gameField.getWorldPosition(true), GameFieldHighlightMode::class)
        }

        player.translateMessage("blocko.setup.reset.step.success.generic", Placeholder.parsed("key", this.key))
    }

}