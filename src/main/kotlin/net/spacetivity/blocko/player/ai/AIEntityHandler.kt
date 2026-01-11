package net.spacetivity.blocko.player.ai

import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.player.EntityPickRule
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.player.ai.impl.*

/**
 * AI Entity Handler - Decision Logic by Tobias Heimböck
 *
 * Handles AI decision-making for game entities using a weighted rule-based system.
 *
 * Each rule is associated with:
 * - a weight (Int): Priority level - higher weight rules take precedence
 * - a probability (Double): Used when multiple rules have the same weight
 *
 * Rule evaluation:
 * - Rules are sorted by weight (descending) for optimal evaluation order
 * - If one rule has a higher weight, it takes precedence.
 * - If both rules have the same weight:
 *     - A random number from 1 to 10 is generated.
 *     - If the number is greater than 5, the rule with the higher probability wins.
 *     - If the number is 5 or lower, one of the two rules is chosen at random.
 *
 * Performance optimizations:
 * - Early exit when maximum weight (5) is reached
 * - Rules sorted by weight to prioritize high-priority checks
 * - Early fallback check for spawn-only scenarios
 *
 * This logic introduces strategic randomness, making the AI less predictable and more dynamic.
 */
class AIEntityHandler {

    companion object {
        private const val RANDOM_RANGE = 10
        private const val RANDOM_THRESHOLD = 5
        private val MAX_WEIGHT = EntityPickRule.MOVABLE_OUT_OF_START.weight
    }

    private val aiGamePlayRules = listOf(
        MovableOutOfStartRule(),
        MovableAwayFromFirstFieldRule(),
        MovableButLandsAfterOpponentRule(),
        MovableButDangerousRule(),
        MovableAndTargetInSightRule(),
        MovableAndGarageEntrancePossibleRule(),
        MovableRule()
    ).sortedByDescending { it.weight }

    fun analyzeSituation(gamePlayer: GamePlayer, dicedNumber: Int): AIResult {
        val gameEntities = Blocko.instance.gameEntityHandler.getEntitiesFromTeam(gamePlayer.arenaId, gamePlayer.teamName!!)
        
        // Early fallback: when all entities are at spawn, only move if dice is 6
        if (gameEntities.all { it.isAtSpawn() }) {
            return if (dicedNumber == 6) {
                AIResult(EntityPickRule.MOVABLE_OUT_OF_START, gameEntities.random())
            } else {
                AIResult(EntityPickRule.NOT_MOVABLE, null)
            }
        }
        
        val startField = Blocko.Companion.instance.gameFieldHandler.getFirstFieldForTeam(gamePlayer.arenaId, gamePlayer.teamName!!)
            ?: return AIResult(EntityPickRule.NOT_MOVABLE, null)
        
        var bestRule = AIResult(EntityPickRule.NOT_MOVABLE, null)

        for (entity in gameEntities) {
            for (rule in aiGamePlayRules) {
                if (rule.evaluate(entity, gamePlayer, dicedNumber, startField)) {
                    val candidate = rule.result(entity)
                    if (candidate.rule.weight > bestRule.rule.weight ||
                        (candidate.rule.weight == bestRule.rule.weight && shouldPreferCandidate(candidate, bestRule))
                    ) {
                        bestRule = candidate
                        
                        // Early exit: if we found the highest priority rule, no need to check others
                        if (bestRule.rule.weight == MAX_WEIGHT) {
                            return bestRule
                        }
                    }
                }
            }
        }

        return bestRule
    }

    /**
     * Determines whether to prefer a candidate rule over the current best rule when they have the same weight.
     * Uses probabilistic logic with randomness to make AI decisions less predictable.
     *
     * @param candidate The candidate rule to evaluate
     * @param current The current best rule
     * @return true if the candidate should be preferred, false otherwise
     */
    private fun shouldPreferCandidate(candidate: AIResult, current: AIResult): Boolean {
        val randomValue = (1..RANDOM_RANGE).random()
        return if (randomValue > RANDOM_THRESHOLD) {
            // When random value is high: prefer rule with higher probability
            candidate.rule.probability > current.rule.probability
        } else {
            // When random value is low: random choice between both rules
            randomValue % 2 == 0
        }
    }
}