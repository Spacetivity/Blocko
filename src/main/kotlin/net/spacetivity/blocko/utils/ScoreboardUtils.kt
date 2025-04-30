package net.spacetivity.blocko.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team

object ScoreboardUtils {

    fun registerScoreboardTeam(teamName: String, color: NamedTextColor): Team {
        val scoreboard = Bukkit.getScoreboardManager().mainScoreboard

        var sbTeam = scoreboard.getTeam(teamName)
        if (sbTeam == null) sbTeam = scoreboard.registerNewTeam(teamName)

        sbTeam.color(color)
        sbTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER)
        return sbTeam
    }

    fun registerScoreboardTeamWithContent(scoreboard: Scoreboard, teamName: String, prefix: Component, suffix: Component): Team {
        var sbTeam = scoreboard.getTeam(teamName)
        if (sbTeam == null) sbTeam = scoreboard.registerNewTeam(teamName)

        sbTeam.prefix(prefix)
        sbTeam.suffix(suffix)

        return sbTeam
    }

}