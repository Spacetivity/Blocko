package net.spacetivity.blocko.arena.setup.step.impl.step

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.arena.setup.SetupTool.ToolModeKeybind
import net.spacetivity.blocko.arena.setup.SetupTool.ToolModeKeybindHint
import net.spacetivity.blocko.arena.setup.getSetupSession
import net.spacetivity.blocko.arena.setup.step.SetupStep
import net.spacetivity.blocko.arena.setup.step.impl.reset.TeamPathResetData
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.translation.translateMessage
import org.bukkit.Material
import org.bukkit.entity.Player

class SetTeamPathsStep : SetupStep<TeamPathResetData> {

    override val id = 3
    override val key = "SetTeamPath"

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

    override fun reset(player: Player, optionalData: TeamPathResetData?) {
        val setupSession = player.getSetupSession() ?: return
        val scanBoardStep = setupSession.getSetupStep<ScanBoardStep>() ?: return

        if (optionalData == null) {
            if (scanBoardStep.gameFields.isEmpty()) {
                player.translateMessage("blocko.setup.reset.step.no_fields")
                return
            }

            for (gameField in scanBoardStep.gameFields) {
                gameField.properties.teamPathIds.clear()
                removeHighlightEntity(gameField)
            }

            player.translateMessage("blocko.setup.reset.step.success.generic", Placeholder.parsed("step", this.key))
        } else {
            val teamName = optionalData.teamName
            val fieldAmount = optionalData.fieldAmount

            val fieldsWithTeamPathId = mutableListOf<GameField>()

            for (gameField in scanBoardStep.gameFields) {
                if (gameField.properties.getTeamPathId(teamName) == null) continue
                fieldsWithTeamPathId.add(gameField)
            }

            val fieldsTorRemoveTeamPath = if (fieldAmount == -1) {
                fieldsWithTeamPathId
            } else {
                fieldsWithTeamPathId.sortedByDescending { it.properties.getTeamPathId(teamName) }.take(fieldAmount)
            }

            if ((fieldAmount > fieldsWithTeamPathId.size) || fieldsWithTeamPathId.isEmpty() || fieldsTorRemoveTeamPath.isEmpty()) {
                player.translateMessage("blocko.setup.reset.step.field_amount_to_big")
                return
            }

            for (gameField in fieldsTorRemoveTeamPath) {
                gameField.properties.removeTeamPathId(teamName)
                removeHighlightEntity(gameField)
            }

            val gameTeam = Blocko.instance.gameTeamHandler.getTeam(setupSession.arenaId, optionalData.teamName)
                ?: return
            player.translateMessage("blocko.setup.reset.step.success.team_paths", Placeholder.parsed("step", this.key),
                Placeholder.parsed("team_color", "<${gameTeam.color.asHexString()}>"),
                Placeholder.parsed("team_name", gameTeam.name.lowercase().replaceFirstChar { it.uppercase() }))
        }
    }

    private fun removeHighlightEntity(gameField: GameField) {
        val highlightMode = gameField.currentHighlightMode ?: return
        Blocko.instance.gameFieldHighlightHandler.spawnOrUpdateHighlightEntity(gameField.arenaId, gameField.getWorldPosition(true), highlightMode)
    }

}