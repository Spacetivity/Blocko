package net.spacetivity.blocko.player.ai

import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.player.GamePlayer

/**
 * Interface for AI decision rules used by the Blocko AI system.
 * 
 * Each rule represents a strategic decision pattern that evaluates whether an entity should be moved
 * based on specific game conditions. Rules are prioritized by weight and probability.
 * 
 * @property weight The priority weight of this rule (higher = more important). Rules with higher weight
 *                  always take precedence over rules with lower weight.
 * @property probability The probability value used when multiple rules have the same weight. Higher
 *                      probability values are preferred when weight is equal.
 * 
 * @see AIEntityHandler for the rule evaluation system
 */
interface AIRule {
    val weight: Int
    val probability: Double

    /**
     * Evaluates whether this rule applies to the given entity and game situation.
     * 
     * @param entity The game entity to evaluate
     * @param player The AI player making the decision
     * @param dicedNumber The number rolled on the dice
     * @param startField The first field of the player's team path
     * @return true if this rule applies to the given situation, false otherwise
     */
    fun evaluate(entity: GameEntity, player: GamePlayer, dicedNumber: Int, startField: GameField): Boolean
    
    /**
     * Returns the AI result for this rule applied to the given entity.
     * 
     * @param entity The game entity this rule applies to
     * @return An AIResult containing the rule type and selected entity
     */
    fun result(entity: GameEntity): AIResult
}