package net.spacetivity.blocko.field.highlighting.scoreboard.impl

import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.blocko.field.highlighting.scoreboard.HighlightMode

class GameFieldHighlightMode : HighlightMode("game_field_highlight") {

    override val color: NamedTextColor = NamedTextColor.WHITE

}