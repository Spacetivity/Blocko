package net.spacetivity.blocko.player.ai.impl

import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.player.EntityPickRule
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.player.ai.AIRule
import net.spacetivity.blocko.player.ai.AIResult

/**
 * AI Rule: Movable Away From First Field
 * 
 * High priority rule (weight: 4). Triggers when an entity is on the first field (field 0) and can move.
 * 
 * Conditions:
 * - Entity must be on field 0 (first field of the team's path)
 * - Entity must be movable with the given dice number
 * 
 * Strategy: Moving entities away from the vulnerable first field prevents opponents from easily eliminating them.
 * This prioritizes protecting entities that are just starting their journey.
 */
class MovableAwayFromFirstFieldRule : AIRule {
    override val weight = EntityPickRule.MOVABLE_AWAY_FROM_FIRST_FIELD.weight
    override val probability = EntityPickRule.MOVABLE_AWAY_FROM_FIRST_FIELD.probability

    override fun evaluate(entity: GameEntity, player: GamePlayer, dicedNumber: Int, startField: GameField): Boolean {
        return entity.currentFieldId == 0 && entity.isMovableTo(dicedNumber)
    }

    override fun result(entity: GameEntity): AIResult {
        return AIResult(EntityPickRule.MOVABLE_AWAY_FROM_FIRST_FIELD, entity)
    }
}