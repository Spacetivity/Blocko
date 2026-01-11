package net.spacetivity.blocko.player.ai.impl

import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.player.EntityPickRule
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.player.ai.AIRule
import net.spacetivity.blocko.player.ai.AIResult

/**
 * AI Rule: Movable And Target In Sight
 * 
 * Medium priority rule (weight: 3, probability: 0.4). Triggers when an entity can eliminate an opponent.
 * 
 * Conditions:
 * - Entity must be movable with the given dice number
 * - Entity's goal field contains an opponent's entity (target)
 * 
 * Strategy: Eliminating opponents is advantageous but not always the best choice. The lower probability (0.4)
 * means this rule competes with other weight-3 rules probabilistically, adding variability to AI behavior.
 */
class MovableAndTargetInSightRule : AIRule {
    override val weight = EntityPickRule.MOVABLE_AND_TARGET_IN_SIGHT.weight
    override val probability = EntityPickRule.MOVABLE_AND_TARGET_IN_SIGHT.probability

    override fun evaluate(entity: GameEntity, player: GamePlayer, dicedNumber: Int, startField: GameField): Boolean {
        return entity.isMovableTo(dicedNumber) && entity.hasTargetAtGoalField(dicedNumber)
    }

    override fun result(entity: GameEntity): AIResult {
        return AIResult(EntityPickRule.MOVABLE_AND_TARGET_IN_SIGHT, entity)
    }
}