package net.spacetivity.blocko.field.highlighting.impl

import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.blocko.field.highlighting.HighlightMode

class GameEntityGoalFieldHighlightMode : HighlightMode("game_entity_goal_field_highlight") {

    override val color: NamedTextColor = NamedTextColor.GOLD

}