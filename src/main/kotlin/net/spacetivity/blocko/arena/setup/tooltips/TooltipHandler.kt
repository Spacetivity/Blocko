package net.spacetivity.blocko.arena.setup.tooltips

import net.spacetivity.blocko.arena.setup.step.SetupStep
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class TooltipHandler {

    private val tooltips = mutableSetOf<Tooltip>()

    fun registerTooltip(tooltip: Tooltip) {
        this.tooltips.add(tooltip)
    }

    fun getTooltip(setupStep: SetupStep<*>): Tooltip? {
        return this.tooltips.find { it.setupStepId == setupStep.id }
    }

    fun addViewedTooltip(uuid: UUID, setupStepId: Int) {
        transaction {
            TooltipViewersDAO.insert { statement ->
                statement[this.uuid] = uuid.toString()
                statement[this.setupStepId] = setupStepId
            }
        }
    }

    fun hasViewedTooltip(uuid: UUID, setupStepId: Int): Boolean {
        return transaction {
            TooltipViewersDAO.selectAll().where { (TooltipViewersDAO.uuid eq uuid.toString()) and (TooltipViewersDAO.setupStepId eq setupStepId) }.count() > 0
        }
    }

}