package net.spacetivity.blocko.player.ai

import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.player.EntityPickRule
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.player.ai.impl.*

/*
 * Blocko-Bots – Decision Logic by Tobias Heimböck
 *
 * Each rule is associated with:
 * - a weight (Int)
 * - a probability (Double)
 *
 * Rule evaluation:
 * - If one rule has a higher weight, it takes precedence.
 * - If both rules have the same weight:
 *     - A random number from 1 to 10 is generated.
 *     - If the number is greater than 5, the rule with the higher probability wins.
 *     - If the number is 5 or lower, one of the two rules is chosen at random.
 *
 * This logic introduces strategic randomness, making the AI less predictable and more dynamic.
 */
class AIEntityHandler {

    private val aiGamePlayRules = listOf(
        MovableOutOfStartRule(),
        MovableAwayFromFirstFieldRule(),
        MovableButLandsAfterOpponentRule(),
        MovableAndTargetInSightRule(),
        MovableAndGarageEntrancePossibleRule(),
        MovableRule()
    )

    fun analyzeSituation(gamePlayer: GamePlayer, dicedNumber: Int): AIResult {
        val gameEntities = Blocko.Companion.instance.gameEntityHandler.getEntitiesFromTeam(gamePlayer.arenaId, gamePlayer.teamName!!)
        val startField = Blocko.Companion.instance.gameFieldHandler.getFirstFieldForTeam(gamePlayer.arenaId, gamePlayer.teamName!!)!!
        var bestRule = AIResult(EntityPickRule.NOT_MOVABLE, null)

        for (entity in gameEntities) {
            for (rule in aiGamePlayRules) {
                if (rule.evaluate(entity, gamePlayer, dicedNumber, startField)) {
                    val candidate = rule.result(entity)
                    if (candidate.rule.weight > bestRule.rule.weight ||
                        (candidate.rule.weight == bestRule.rule.weight &&
                                (1..10).random() > 5 && candidate.rule.probability > bestRule.rule.probability)
                    ) {
                        bestRule = candidate
                    }
                }
            }
        }

        // Fallback: when all entities are at spawn.
        if (gameEntities.all { it.isAtSpawn() }) {
            return if (dicedNumber == 6) AIResult(EntityPickRule.MOVABLE_OUT_OF_START, gameEntities.random())
            else AIResult(EntityPickRule.NOT_MOVABLE, null)
        }

        return bestRule
    }
}