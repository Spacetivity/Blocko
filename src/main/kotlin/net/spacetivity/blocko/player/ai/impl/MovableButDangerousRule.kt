package net.spacetivity.blocko.player.ai.impl

import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.player.EntityPickRule
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.player.ai.AIRule
import net.spacetivity.blocko.player.ai.AIResult

/**
 * AI Rule: Movable But Dangerous
 * 
 * Lowest priority defensive rule (weight: 1, probability: 0.3). Triggers when an entity can move
 * but would land on a dangerous position (after an opponent).
 * 
 * Conditions:
 * - Entity must be movable with the given dice number
 * - Entity would land on a field after an opponent (landsAfterOpponent)
 * 
 * Strategy: This is a defensive rule that recognizes risky moves. It has the same weight as
 * MOVABLE_BUT_LANDS_AFTER_OPPONENT but lower probability (0.3), making it less likely to be chosen
 * when both rules match. This encourages the AI to avoid dangerous positions when safer alternatives exist.
 * 
 * Note: This rule complements MovableButLandsAfterOpponentRule by providing a lower-probability
 * alternative for the same situation, allowing for more varied defensive behavior.
 */
class MovableButDangerousRule : AIRule {
    override val weight = EntityPickRule.MOVABLE_BUT_LANDS_AFTER_OPPONENT.weight
    override val probability = 0.3 // Lower than MOVABLE_BUT_LANDS_AFTER_OPPONENT (0.4)

    override fun evaluate(entity: GameEntity, player: GamePlayer, dicedNumber: Int, startField: GameField): Boolean {
        return entity.isMovableTo(dicedNumber) && entity.landsAfterOpponent(dicedNumber)
    }

    override fun result(entity: GameEntity): AIResult {
        return AIResult(EntityPickRule.MOVABLE_BUT_LANDS_AFTER_OPPONENT, entity)
    }
}

