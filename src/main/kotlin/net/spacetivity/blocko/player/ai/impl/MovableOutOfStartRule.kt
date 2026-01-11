package net.spacetivity.blocko.player.ai.impl

import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.player.EntityPickRule
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.player.ai.AIRule
import net.spacetivity.blocko.player.ai.AIResult

/**
 * AI Rule: Movable Out Of Start
 * 
 * Highest priority rule (weight: 5). Triggers when an entity is still at spawn and can be moved out.
 * 
 * Conditions:
 * - Entity must be at spawn (currentFieldId == null)
 * - Dice number must be 6 (required to leave spawn)
 * - Start field must be free or not occupied by own team
 * 
 * Strategy: Getting entities out of spawn is critical for progress, so this takes absolute precedence.
 */
class MovableOutOfStartRule : AIRule {
    override val weight = EntityPickRule.MOVABLE_OUT_OF_START.weight
    override val probability = EntityPickRule.MOVABLE_OUT_OF_START.probability

    override fun evaluate(entity: GameEntity, player: GamePlayer, dicedNumber: Int, startField: GameField): Boolean {
        return entity.isAtSpawn() &&
                dicedNumber == 6 &&
                (startField.currentHolder == null || startField.currentHolder?.teamName != player.teamName)
    }

    override fun result(entity: GameEntity): AIResult {
        return AIResult(EntityPickRule.MOVABLE_OUT_OF_START, entity)
    }
}