package net.spacetivity.blocko.player.ai.impl

import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.player.EntityPickRule
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.player.ai.AIRule
import net.spacetivity.blocko.player.ai.AIResult

/**
 * AI Rule: Movable
 * 
 * Basic priority rule (weight: 2, probability: 0.6). Triggers when an entity can simply move forward.
 * 
 * Conditions:
 * - Entity must be movable with the given dice number
 * 
 * Strategy: This is the fallback rule for any entity that can move but doesn't match higher-priority conditions.
 * It ensures forward progress when no strategic advantage is available. The probability of 0.6 means it's
 * preferred over risky moves when weight is equal.
 */
class MovableRule : AIRule {

    override val weight: Int = EntityPickRule.MOVABLE.weight
    override val probability: Double = EntityPickRule.MOVABLE.probability

    override fun evaluate(entity: GameEntity, player: GamePlayer, dicedNumber: Int, startField: GameField): Boolean {
        return entity.isMovableTo(dicedNumber)
    }

    override fun result(entity: GameEntity): AIResult {
        return AIResult(EntityPickRule.MOVABLE, entity)
    }

}