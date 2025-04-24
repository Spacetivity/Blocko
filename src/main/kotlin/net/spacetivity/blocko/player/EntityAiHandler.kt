package net.spacetivity.blocko.player

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.player.ai.impl.*

/*
 * Blocko-Bots – Decision Logic by Tobias Heimböck (TGamings)
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
class EntityAiHandler {

    private val aiRules = listOf(
        MovableOutOfStartRule(),
        MovableAwayFromFirstFieldRule(),
        MovableButLandsAfterOpponentRule(),
        MovableAndTargetInSightRule(),
        MovableAndGarageEntrancePossibleRule(),
        MovableRule()
    )

    fun analyzeCurrentRuleSituation(gamePlayer: GamePlayer, dicedNumber: Int): Pair<EntityPickRule, GameEntity?> {
        val gameEntities = BlockoGame.instance.gameEntityHandler.getEntitiesFromTeam(gamePlayer.arenaId, gamePlayer.teamName!!)
        val startField = BlockoGame.instance.gameFieldHandler.getFirstFieldForTeam(gamePlayer.arenaId, gamePlayer.teamName!!)!!
        var bestRule: Pair<EntityPickRule, GameEntity?> = Pair(EntityPickRule.NOT_MOVABLE, null)

        for (entity in gameEntities) {
            for (rule in aiRules) {
                if (rule.evaluate(entity, gamePlayer, dicedNumber, startField)) {
                    val candidate = rule.result(entity)
                    if (candidate.first.weight > bestRule.first.weight ||
                        (candidate.first.weight == bestRule.first.weight &&
                                (1..10).random() > 5 && candidate.first.probability > bestRule.first.probability)
                    ) {
                        bestRule = candidate
                    }
                }
            }
        }

        // Fallback: when all entities are at spawn.
        if (gameEntities.all { it.isAtSpawn() }) {
            return if (dicedNumber == 6) Pair(EntityPickRule.MOVABLE_OUT_OF_START, gameEntities.random())
            else Pair(EntityPickRule.NOT_MOVABLE, null)
        }

        return bestRule
    }
}