package net.spacetivity.blocko.field.highlighting

import net.spacetivity.blocko.arena.id.ArenaId
import java.util.*

data class HighlightEntity(val uuid: UUID, val arenaId: ArenaId, val highlightMode: HighlightMode)