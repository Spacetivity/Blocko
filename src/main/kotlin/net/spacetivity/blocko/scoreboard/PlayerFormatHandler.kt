package net.spacetivity.blocko.scoreboard

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.extensions.toGamePlayerInstance
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.utils.ScoreboardUtils
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team
import java.util.*

class PlayerFormatHandler {

    fun setTablistFormatForAll() {
        Bukkit.getOnlinePlayers().forEach { setTablistFormatForPlayer(it) }
    }

    private fun setTablistFormatForPlayer(newPlayer: Player) {
        Bukkit.getOnlinePlayers().forEach { setTablistFormat(it, it.scoreboard) }
    }

    private fun setTablistFormat(player: Player, scoreboard: Scoreboard) {
        val gamePlayer: GamePlayer? = player.toGamePlayerInstance()
        val teamName: String = "0_${if (gamePlayer == null) "lobby" else if (gamePlayer.teamName == null) "lobby" else gamePlayer.teamName}_0_${UUID.randomUUID().toString().split("-")[0]}"

        var color: NamedTextColor = NamedTextColor.GRAY

        if (gamePlayer?.teamName != null)
            color = BlockoGame.instance.gameTeamHandler.getTeam(gamePlayer.arenaId, gamePlayer.teamName!!)!!.color

        val prefix: Component = BlockoGame.instance.translationHandler.getSelectedTranslation().validateLine("blocko.format.tablist",
            Placeholder.parsed("color", "<${color.asHexString()}>"))

        val team: Team = ScoreboardUtils.registerScoreboardTeamWithContent(scoreboard, teamName, prefix, Component.text(""))
        team.color(color)

        if (!team.hasEntry(player.name)) team.addEntry(player.name)
    }

}