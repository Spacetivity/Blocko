package net.spacetivity.blocko.scoreboard

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.entity.GameEntityStatus
import net.spacetivity.blocko.extensions.toGamePlayerInstance
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.team.GameTeam
import net.spacetivity.blocko.translation.Translation
import org.bukkit.entity.Player

object GameScoreboardUtils {

    fun setGameSidebar(player: Player) {
        val translation: Translation = BlockoGame.instance.translationHandler.getSelectedTranslation()
        val initialEntityStatus = GameEntityStatus.AT_SPAWN

        BlockoGame.instance.sidebarHandler.registerSidebar(SidebarBuilder(player)
            .setTitle(translation.validateLine("blocko.sidebar.title"))
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

    fun removeGameSidebar(player: Player) {
        BlockoGame.instance.sidebarHandler.unregisterSidebar(player.uniqueId)
    }

    fun updateTeamLine(player: Player) {
        val sidebar: Sidebar = BlockoGame.instance.sidebarHandler.getSidebar(player.uniqueId) ?: return
        sidebar.updateLine(8, getTeamComponent(BlockoGame.instance.translationHandler.getSelectedTranslation(), player.toGamePlayerInstance()))
    }

    fun updateControllingTeamLine(gameArena: GameArena, controllingTeam: GameTeam) {
        for (player: Player in gameArena.getAllPlayers()) {
            val sidebar: Sidebar = BlockoGame.instance.sidebarHandler.getSidebar(player.uniqueId) ?: continue
            sidebar.updateLine(6, getControllingTeamComponent(BlockoGame.instance.translationHandler.getSelectedTranslation(), controllingTeam))
        }
    }

    fun updateAllEntityStatusLines(arenaId: String, newControllingTeam: GameTeam) {
        for (gameEntity: GameEntity in BlockoGame.instance.gameEntityHandler.getEntitiesFromTeam(arenaId, newControllingTeam.name)) {
            updateEntityStatusLine(gameEntity)
        }
    }

    fun updateEntityStatusLine(gameEntity: GameEntity) {
        val gameArena: GameArena = BlockoGame.instance.gameArenaHandler.getArena(gameEntity.arenaId) ?: return

        for (player: Player in gameArena.getAllPlayers()) {
            val sidebar: Sidebar = BlockoGame.instance.sidebarHandler.getSidebar(player.uniqueId) ?: continue
            val lineId: Int = getSidebarLineForEntity(gameEntity.entityId) ?: continue
            sidebar.updateLine(lineId, getStatusComponent(BlockoGame.instance.translationHandler.getSelectedTranslation(), gameEntity.entityId.inc(), gameEntity.entityStatus))
        }
    }

    fun updateDicedNumberLine(arenaId: String, currentDicedNumber: Int?) {
        val gameArena: GameArena = BlockoGame.instance.gameArenaHandler.getArena(arenaId) ?: return
        if (!gameArena.phase.isIngame()) return

        for (player: Player in gameArena.getAllPlayers()) {
            val sidebar: Sidebar = BlockoGame.instance.sidebarHandler.getSidebar(player.uniqueId) ?: continue
            sidebar.updateLine(5, getDicedNumberComponent(BlockoGame.instance.translationHandler.getSelectedTranslation(), currentDicedNumber))
        }
    }

    private fun getTeamComponent(translation: Translation, gamePlayer: GamePlayer?): Component {
        val teamColorHex: String
        val teamName: String

        if (gamePlayer?.teamName == null) {
            teamColorHex = NamedTextColor.GRAY.asHexString()
            teamName = "-/-"
        } else {
            val gameTeam: GameTeam = BlockoGame.instance.gameTeamHandler.getTeam(gamePlayer.arenaId, gamePlayer.teamName!!)!!
            teamColorHex = gameTeam.color.asHexString()
            teamName = gameTeam.name
        }

        return translation.validateLine("blocko.sidebar.line.team_name", Placeholder.parsed("team_color", "<$teamColorHex>"), Placeholder.parsed("team_name", teamName))
    }

    private fun getStatusComponent(translation: Translation, entityId: Int, status: GameEntityStatus): Component {
        return translation.validateLine("blocko.sidebar.line.entity_status", Placeholder.parsed("id", entityId.toString()), Placeholder.parsed("status", status.display))
    }

    private fun getControllingTeamComponent(translation: Translation, gameTeam: GameTeam?): Component {
        if (gameTeam == null) return translation.validateLine("blocko.sidebar.line.controlling_team_name.not_ingame")
        return translation.validateLine("blocko.sidebar.line.controlling_team_name.ingame", Placeholder.parsed("team_color", "<${gameTeam.color.asHexString()}>"), Placeholder.parsed("team_name", gameTeam.name))
    }

    private fun getDicedNumberComponent(translation: Translation, dicedNumber: Int?): Component {
        val placeholder: TagResolver.Single = Placeholder.parsed("number", dicedNumber?.toString() ?: "-/-")
        return translation.validateLine("blocko.sidebar.line.dice_status", placeholder)
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