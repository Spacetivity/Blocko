package net.spacetivity.blocko.player.ai.impl

import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.player.EntityPickRule
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.player.ai.AIRule
import net.spacetivity.blocko.player.ai.AIResult

/**
 * AI Rule: Movable But Lands After Opponent
 * 
 * Low priority rule (weight: 1). Triggers when an entity can move but would land on a field after an opponent.
 * 
 * Conditions:
 * - Entity must be movable with the given dice number
 * - Entity would land on a field that comes after an opponent's entity on the path
 * 
 * Strategy: This is a defensive move - the AI will still move if no better option exists, but it's less preferred
 * as it doesn't directly threaten opponents or advance towards the garage. Used as a fallback option.
 */
class MovableButLandsAfterOpponentRule : AIRule {
    override val weight = EntityPickRule.MOVABLE_BUT_LANDS_AFTER_OPPONENT.weight
    override val probability = EntityPickRule.MOVABLE_BUT_LANDS_AFTER_OPPONENT.probability

    override fun evaluate(entity: GameEntity, player: GamePlayer, dicedNumber: Int, startField: GameField): Boolean {
        return entity.isMovableTo(dicedNumber) && entity.landsAfterOpponent(dicedNumber)
    }

    override fun result(entity: GameEntity): AIResult {
        return AIResult(EntityPickRule.MOVABLE_BUT_LANDS_AFTER_OPPONENT, entity)
    }
}