package net.spacetivity.blocko.arena.setup.step.impl.step

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.arena.setup.SetupTool.ToolModeKeybind
import net.spacetivity.blocko.arena.setup.SetupTool.ToolModeKeybindHint
import net.spacetivity.blocko.arena.setup.getSetupSession
import net.spacetivity.blocko.arena.setup.step.SetupStep
import net.spacetivity.blocko.arena.setup.step.impl.reset.IgnoredResetData
import net.spacetivity.blocko.arena.setup.tooltips.Tooltip
import net.spacetivity.blocko.field.highlighting.impl.GameFieldHighlightMode
import net.spacetivity.blocko.translation.translateMessage
import net.spacetivity.blocko.utils.Constants
import org.bukkit.Material
import org.bukkit.entity.Player

class SetTeamEntrancesStep : SetupStep<IgnoredResetData> {

    override val id = 2
    override val key = "SetTeamEntrance"
    override val keybindHints = setOf(ToolModeKeybindHint(ToolModeKeybind.RIGHT_CLICK, null))
    override val validBlockTypes = setOf(Material.BONE_BLOCK)
    override val tooltip = Tooltip(this.id)
    override var active = false

    override fun getSidebarLines(player: Player): List<Component> {
        val translation = Blocko.instance.translationHandler.getSelectedTranslation()
        val setupSession = player.getSetupSession() ?: return emptyList()
        val scanBoardStep = setupSession.getSetupStep<ScanBoardStep>() ?: return emptyList()

        val lines = mutableListOf<Component>()

        for (gameTeam in Constants.GAME_TEAMS) {
            var teamEntranceConfigured = false

            for (gameField in scanBoardStep.gameFields) {
                if (gameField.properties.teamEntrance == null) continue
                if (gameField.properties.teamEntrance != gameTeam.name) continue
                teamEntranceConfigured = true
            }

            val keyType = if (teamEntranceConfigured) "configured" else "unconfigured"
            val indicator = translation.lineAsString("blocko.sidebar.setup.lines.indicator.$keyType")

            lines.add(translation.line("blocko.sidebar.setup.lines.entrance_step.$keyType",
                Placeholder.parsed("team_color", "<${gameTeam.color.asHexString()}>"),
                Placeholder.parsed("team_name", gameTeam.name.lowercase().replaceFirstChar { it.uppercase() }),
                Placeholder.parsed("indicator", indicator)))
        }

        return lines
    }

    override fun reset(player: Player, optionalData: IgnoredResetData?) {
        val setupSession = player.getSetupSession() ?: return
        val scanBoardStep = setupSession.getSetupStep<ScanBoardStep>() ?: return

        for (gameField in scanBoardStep.gameFields) {
            if (gameField.properties.teamEntrance == null) continue
            gameField.properties.teamEntrance = null
            gameField.properties.rotation = null

            Blocko.instance.gameFieldHighlightHandler.spawnOrUpdateHighlightEntity(gameField.arenaId, gameField.getWorldPosition(true), GameFieldHighlightMode::class)
        }

        player.translateMessage("blocko.setup.reset.step.success.generic", Placeholder.parsed("step", this.key))
    }

}