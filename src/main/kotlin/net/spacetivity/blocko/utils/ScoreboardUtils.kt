package net.spacetivity.blocko.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team
import java.util.*

object ScoreboardUtils {

    fun registerScoreboardTeam(teamName: String, color: NamedTextColor): Team {
        val scoreboard: Scoreboard = Bukkit.getScoreboardManager().mainScoreboard
        val safeTeamName = "${UUID.randomUUID().toString().split("-")[0]}_$teamName"

        var sbTeam: Team? = scoreboard.getTeam(safeTeamName)
        if (sbTeam == null) sbTeam = scoreboard.registerNewTeam(safeTeamName)

        sbTeam.color(color)
        sbTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER)
        return sbTeam
    }

    fun registerScoreboardTeamWithContent(scoreboard: Scoreboard, teamName: String, prefix: Component, suffix: Component): Team {
        var sbTeam: Team? = scoreboard.getTeam(teamName)
        if (sbTeam == null) sbTeam = scoreboard.registerNewTeam(teamName)

        sbTeam.prefix(prefix)
        sbTeam.suffix(suffix)

        return sbTeam
    }

}