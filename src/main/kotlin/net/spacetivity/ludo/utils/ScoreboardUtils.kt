package net.spacetivity.ludo.utils

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

}