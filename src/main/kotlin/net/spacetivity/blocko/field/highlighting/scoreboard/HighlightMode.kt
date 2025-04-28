package net.spacetivity.blocko.field.highlighting.scoreboard

import net.kyori.adventure.text.format.NamedTextColor

abstract class HighlightMode(val teamName: String) {

    abstract val color: NamedTextColor

}