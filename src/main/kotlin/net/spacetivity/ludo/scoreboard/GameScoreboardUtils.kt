package net.spacetivity.ludo.scoreboard

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.entity.GameEntity
import net.spacetivity.ludo.entity.GameEntityStatus
import net.spacetivity.ludo.phase.impl.IngamePhase
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.team.GameTeam
import net.spacetivity.ludo.translation.Translation
import org.bukkit.entity.Player

object GameScoreboardUtils {

    fun setGameSidebar(gamePlayer: GamePlayer) {
        val player: Player = gamePlayer.toBukkitInstance() ?: return
        val translation: Translation = LudoGame.instance.translationHandler.getSelectedTranslation()

        val gameTeam: GameTeam = LudoGame.instance.gameTeamHandler.getTeam(gamePlayer.arenaId, gamePlayer.teamName)
            ?: return

        LudoGame.instance.sidebarHandler.registerSidebar(SidebarBuilder(player)
            .setTitle(translation.validateLine("blocko.sidebar.title"))
            .addBlankLine()
            .addLine(translation.validateLine("blocko.sidebar.line.team_name", Placeholder.parsed("team_color", "<${gameTeam.color.asHexString()}>"), Placeholder.parsed("team_name", gameTeam.name)))
            .addBlankLine()
            .addLine(getControllingTeamComponent(translation, null))
            .addLine(getDicedNumberComponent(translation, null))
            .addBlankLine()
            .addLine(getStatusComponent(translation, 1, GameEntityStatus.AT_SPAWN))
            .addLine(getStatusComponent(translation, 2, GameEntityStatus.AT_SPAWN))
            .addLine(getStatusComponent(translation, 3, GameEntityStatus.AT_SPAWN))
            .addLine(getStatusComponent(translation, 4, GameEntityStatus.AT_SPAWN))
            .build())
    }

    fun removeGameSidebar(player: Player) {
        LudoGame.instance.sidebarHandler.unregisterSidebar(player.uniqueId)
    }

    fun updateControllingTeamLine(gameArena: GameArena, controllingTeam: GameTeam) {
        val translation: Translation = LudoGame.instance.translationHandler.getSelectedTranslation()

        for (gamePlayer: GamePlayer in gameArena.currentPlayers.filter { !it.isAI }) {
            val sidebar: Sidebar = LudoGame.instance.sidebarHandler.getSidebar(gamePlayer.uuid) ?: continue
            sidebar.updateLine(6, getControllingTeamComponent(translation, controllingTeam))
        }
    }

    fun updateAllEntityStatusLines(arenaId: String, newControllingTeam: GameTeam) {
        for (gameEntity: GameEntity in LudoGame.instance.gameEntityHandler.getEntitiesFromTeam(arenaId, newControllingTeam.name)) {
            updateEntityStatusLine(gameEntity)
        }
    }

    fun updateEntityStatusLine(gameEntity: GameEntity) {
        val translation: Translation = LudoGame.instance.translationHandler.getSelectedTranslation()
        val gameArena: GameArena = LudoGame.instance.gameArenaHandler.getArena(gameEntity.arenaId) ?: return

        for (gamePlayer: GamePlayer in gameArena.currentPlayers.filter { !it.isAI }) {
            val sidebar: Sidebar = LudoGame.instance.sidebarHandler.getSidebar(gamePlayer.uuid) ?: continue
            val lineId: Int = getSidebarLineForEntity(gameEntity.entityId) ?: continue
            sidebar.updateLine(lineId, getStatusComponent(translation, gameEntity.entityId.inc(), gameEntity.entityStatus))
        }
    }

    fun updateDicedNumberLine(arenaId: String, currentDicedNumber: Int?) {
        val translation: Translation = LudoGame.instance.translationHandler.getSelectedTranslation()
        val gameArena: GameArena = LudoGame.instance.gameArenaHandler.getArena(arenaId) ?: return

        if (!gameArena.phase.isIngame()) return
        val ingamePhase: IngamePhase = gameArena.phase as IngamePhase

        val controllingTeam: GameTeam = ingamePhase.getControllingTeam() ?: return
        //val controllingPlayer: GamePlayer = gameArena.currentPlayers.find { it.uuid == controllingTeam.teamMembers.first() } ?: return

        for (gamePlayer: GamePlayer in gameArena.currentPlayers.filter { !it.isAI }) {
            val sidebar: Sidebar = LudoGame.instance.sidebarHandler.getSidebar(gamePlayer.uuid) ?: continue
            sidebar.updateLine(5, getDicedNumberComponent(translation, currentDicedNumber))
        }
    }

    private fun getStatusComponent(translation: Translation, entityId: Int, status: GameEntityStatus): Component {
        return translation.validateLine("blocko.sidebar.line.entity_status", Placeholder.parsed("id", entityId.toString()), Placeholder.parsed("status", status.display))
    }

    private fun getControllingTeamComponent(translation: Translation, gameTeam: GameTeam?): Component {
        if (gameTeam == null) return translation.validateLine("blocko.sidebar.line.turn.notIngame")
        return translation.validateLine("blocko.sidebar.line.turn", Placeholder.parsed("team_color", "<${gameTeam.color.asHexString()}>"), Placeholder.parsed("team_name", gameTeam.name))
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