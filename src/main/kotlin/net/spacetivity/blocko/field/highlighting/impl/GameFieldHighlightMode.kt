package net.spacetivity.blocko.field.highlighting.impl

import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.blocko.field.highlighting.HighlightMode

class GameFieldHighlightMode : HighlightMode("game_field_highlight") {

    override val color: NamedTextColor = NamedTextColor.WHITE

}