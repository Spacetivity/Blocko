package net.spacetivity.blocko.arena.setup.tooltips

import org.jetbrains.exposed.sql.Table

object TooltipViewersDAO : Table("tooltip_viewers") {
    val uuid = varchar("uuid", 36)
    val setupStepId = integer("setup_step_id")

    init {
        uniqueIndex(uuid, setupStepId)
    }
}