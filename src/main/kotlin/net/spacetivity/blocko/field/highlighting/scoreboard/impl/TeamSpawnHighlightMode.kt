package net.spacetivity.blocko.field.highlighting.scoreboard.impl

import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.blocko.field.highlighting.scoreboard.HighlightMode
import net.spacetivity.blocko.team.GameTeam

class TeamSpawnHighlightMode(private val gameTeam: GameTeam) : HighlightMode("team_spawn_highlight") {

    override val color: NamedTextColor = this.gameTeam.color

}