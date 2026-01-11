package net.spacetivity.blocko.field.highlighting.impl

import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.blocko.field.highlighting.HighlightMode
import net.spacetivity.blocko.team.GameTeam

class TeamPathHighlightMode(private val gameTeam: GameTeam) : HighlightMode("${gameTeam.name}_path_highlight") {

    override val color: NamedTextColor = this.gameTeam.color

}