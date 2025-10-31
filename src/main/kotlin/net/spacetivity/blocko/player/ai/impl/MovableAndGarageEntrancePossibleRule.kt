package net.spacetivity.blocko.player.ai.impl

import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.player.EntityPickRule
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.player.ai.AIRule
import net.spacetivity.blocko.player.ai.AIResult

/**
 * AI Rule: Movable And Garage Entrance Possible
 * 
 * Medium priority rule (weight: 3, probability: 0.6). Triggers when an entity can reach the garage entrance.
 * 
 * Conditions:
 * - Entity must be movable with the given dice number
 * - Entity's path to the goal field includes at least one garage field
 * 
 * Strategy: Getting entities into the garage is important for winning. This rule has higher probability (0.6)
 * than target elimination, prioritizing advancement over aggression when both options are available.
 */
class MovableAndGarageEntrancePossibleRule : AIRule {
    override val weight = EntityPickRule.MOVABLE_AND_GARAGE_ENTRANCE_POSSIBLE.weight
    override val probability = EntityPickRule.MOVABLE_AND_GARAGE_ENTRANCE_POSSIBLE.probability

    override fun evaluate(entity: GameEntity, player: GamePlayer, dicedNumber: Int, startField: GameField): Boolean {
        return entity.isMovableTo(dicedNumber) && entity.isGarageInSight(dicedNumber)
    }

    override fun result(entity: GameEntity): AIResult {
        return AIResult(EntityPickRule.MOVABLE_AND_GARAGE_ENTRANCE_POSSIBLE, entity)
    }
}