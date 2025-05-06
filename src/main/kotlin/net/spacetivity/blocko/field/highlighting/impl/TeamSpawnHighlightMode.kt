package net.spacetivity.blocko.field.highlighting.impl

import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.blocko.field.highlighting.HighlightMode
import net.spacetivity.blocko.team.GameTeam

class TeamSpawnHighlightMode(private val gameTeam: GameTeam) : HighlightMode("${gameTeam.name}_spawn_highlight") {

    override val color: NamedTextColor = this.gameTeam.color

}