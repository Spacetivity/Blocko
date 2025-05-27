package net.spacetivity.blocko.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.arena.Arena
import net.spacetivity.blocko.arena.id.ArenaId
import net.spacetivity.blocko.arena.setup.ArenaSetupSession
import net.spacetivity.blocko.arena.setup.getSetupSession
import net.spacetivity.blocko.arena.setup.step.SetupStep
import net.spacetivity.blocko.arena.toGamePlayerInstance
import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.entity.GameEntityStatus
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.scoreboard.SidebarBuilder
import net.spacetivity.blocko.team.GameTeam
import net.spacetivity.blocko.translation.Translation
import org.bukkit.entity.Player

object ScoreboardUtils {

    fun setSetupSidebar(player: Player) {
        val translation = Blocko.instance.translationHandler.getSelectedTranslation()

        val setupSession = player.getSetupSession() ?: return
        val activeSetupStep = setupSession.getActiveSetupStep() ?: return

        val sidebarBuilder = SidebarBuilder(player)
        
        sidebarBuilder.setTitle(getSetupSidebarTitle(setupSession, activeSetupStep, translation))
        
        sidebarBuilder.addBlankLine()
        sidebarBuilder.addLine(translation.line("blocko.sidebar.setup.lines.active_setup_step", Placeholder.parsed("step", activeSetupStep.key)))
        sidebarBuilder.addBlankLine()

        val lines = activeSetupStep.getSidebarLines(player)

        if (lines.isNotEmpty()) {
            sidebarBuilder.addLine(translation.line("blocko.sidebar.setup.lines.active_setup_step_data"))
            sidebarBuilder.addBlankLine()
        }

        sidebarBuilder.addLine(translation.line("blocko.sidebar.setup.lines.placeholder"))
        sidebarBuilder.addLine(translation.line("blocko.sidebar.setup.lines.placeholder"))
        sidebarBuilder.addLine(translation.line("blocko.sidebar.setup.lines.placeholder"))
        sidebarBuilder.addLine(translation.line("blocko.sidebar.setup.lines.placeholder"))

        Blocko.instance.sidebarHandler.registerSidebar(sidebarBuilder.build())
    }

    fun setGameSidebar(player: Player) {
        val translation = Blocko.instance.translationHandler.getSelectedTranslation()
        val initialEntityStatus = GameEntityStatus.AT_SPAWN

        Blocko.instance.sidebarHandler.registerSidebar(SidebarBuilder(player)
            .setTitle(translation.line("blocko.sidebar.game.title"))
            .addBlankLine()
            .addLine(getTeamComponent(translation, player.toGamePlayerInstance()))
            .addBlankLine()
            .addLine(getControllingTeamComponent(translation, null))
            .addLine(getDicedNumberComponent(translation, null))
            .addBlankLine()
            .addLine(getStatusComponent(translation, 1, initialEntityStatus))
            .addLine(getStatusComponent(translation, 2, initialEntityStatus))
            .addLine(getStatusComponent(translation, 3, initialEntityStatus))
            .addLine(getStatusComponent(translation, 4, initialEntityStatus))
            .build())
    }

    fun removeSidebar(player: Player) {
        Blocko.instance.sidebarHandler.unregisterSidebar(player.uniqueId)
    }

    fun updateSetupSidebarTitle(player: Player) {
        val translation = Blocko.instance.translationHandler.getSelectedTranslation()
        val setupSession = player.getSetupSession() ?: return
        val activeSetupStep = setupSession.getActiveSetupStep() ?: return
        val sidebar = Blocko.instance.sidebarHandler.getSidebar(player.uniqueId) ?: return

        sidebar.updateTitle(getSetupSidebarTitle(setupSession, activeSetupStep, translation))
    }

    fun updateSetupDataLines(player: Player, setupStep: SetupStep<*>) {
        val translation = Blocko.instance.translationHandler.getSelectedTranslation()
        val setupSession = player.getSetupSession() ?: return
        val activeSetupStep = setupSession.getActiveSetupStep() ?: return
        val sidebar = Blocko.instance.sidebarHandler.getSidebar(player.uniqueId) ?: return

        sidebar.updateLine(5, translation.line("blocko.sidebar.setup.lines.active_setup_step", Placeholder.parsed("step", activeSetupStep.key)))

        val sidebarLines = setupStep.getSidebarLines(player)

        for (index in 0 until 4) {
            val line = if (sidebarLines.isEmpty()) translation.line("blocko.sidebar.setup.lines.placeholder") else sidebarLines[index]
            sidebar.updateLine(index, line)
        }
    }

    fun updateTeamLine(player: Player) {
        val sidebar = Blocko.instance.sidebarHandler.getSidebar(player.uniqueId) ?: return
        sidebar.updateLine(8, getTeamComponent(Blocko.instance.translationHandler.getSelectedTranslation(), player.toGamePlayerInstance()))
    }

    fun updateControllingTeamLine(arena: Arena, controllingTeam: GameTeam) {
        for (player in arena.getAllPlayers()) {
            val sidebar = Blocko.instance.sidebarHandler.getSidebar(player.uniqueId) ?: continue
            sidebar.updateLine(6, getControllingTeamComponent(Blocko.instance.translationHandler.getSelectedTranslation(), controllingTeam))
        }
    }

    fun updateAllEntityStatusLines(arenaId: ArenaId, newControllingTeam: GameTeam) {
        for (gameEntity in Blocko.instance.gameEntityHandler.getEntitiesFromTeam(arenaId, newControllingTeam.name)) {
            updateEntityStatusLine(gameEntity)
        }
    }

    fun updateEntityStatusLine(gameEntity: GameEntity) {
        val gameArena = Blocko.instance.arenaHandler.getArena(gameEntity.arenaId) ?: return

        for (player in gameArena.getAllPlayers()) {
            val sidebar = Blocko.instance.sidebarHandler.getSidebar(player.uniqueId) ?: continue
            val lineId = getSidebarLineForEntity(gameEntity.entityId) ?: continue
            sidebar.updateLine(lineId, getStatusComponent(Blocko.instance.translationHandler.getSelectedTranslation(), gameEntity.entityId.inc(), gameEntity.entityStatus))
        }
    }

    fun updateDicedNumberLine(arenaId: ArenaId, currentDicedNumber: Int?) {
        val gameArena = Blocko.instance.arenaHandler.getArena(arenaId) ?: return
        if (!gameArena.phase.isIngame()) return

        for (player in gameArena.getAllPlayers()) {
            val sidebar = Blocko.instance.sidebarHandler.getSidebar(player.uniqueId) ?: continue
            sidebar.updateLine(5, getDicedNumberComponent(Blocko.instance.translationHandler.getSelectedTranslation(), currentDicedNumber))
        }
    }

    private fun getSetupSidebarTitle(setupSession: ArenaSetupSession, activeSetupStep: SetupStep<*>, translation: Translation): Component {
        return translation.line("blocko.sidebar.setup.title",
            Placeholder.parsed("current_step_id", activeSetupStep.id.inc().toString()),
            Placeholder.parsed("max_step_id", setupSession.setupSteps.maxOf { it.value.id }.inc().toString()))
    }

    private fun getTeamComponent(translation: Translation, gamePlayer: GamePlayer?): Component {
        val teamColorHex: String
        val teamName: String

        if (gamePlayer?.teamName == null) {
            teamColorHex = NamedTextColor.GRAY.asHexString()
            teamName = Constants.PLACEHOLDER
        } else {
            val gameTeam: GameTeam = Blocko.instance.gameTeamHandler.getTeam(gamePlayer.arenaId, gamePlayer.teamName!!)!!
            teamColorHex = gameTeam.color.asHexString()
            teamName = gameTeam.name
        }

        return translation.line("blocko.sidebar.game.lines.team_name", Placeholder.parsed("team_color", "<$teamColorHex>"), Placeholder.parsed("team_name", teamName))
    }

    private fun getStatusComponent(translation: Translation, entityId: Int, status: GameEntityStatus): Component {
        return translation.line("blocko.sidebar.game.lines.entity_status", Placeholder.parsed("id", entityId.toString()), Placeholder.parsed("status", status.display))
    }

    private fun getControllingTeamComponent(translation: Translation, gameTeam: GameTeam?): Component {
        if (gameTeam == null) return translation.line("blocko.sidebar.line.controlling_team_name.not_ingame")
        return translation.line("blocko.sidebar.game.lines.controlling_team_name.ingame", Placeholder.parsed("team_color", "<${gameTeam.color.asHexString()}>"), Placeholder.parsed("team_name", gameTeam.name))
    }

    private fun getDicedNumberComponent(translation: Translation, dicedNumber: Int?): Component {
        val placeholder = Placeholder.parsed("number", dicedNumber?.toString() ?: Constants.PLACEHOLDER)
        return translation.line("blocko.sidebar.game.lines.dice_status", placeholder)
    }

    private fun getSidebarLineForEntity(entityId: Int): Int? {
        return when (entityId) {
            0 -> 3
            1 -> 2
            2 -> 1
            3 -> 0
            else -> null
        }
    }

}