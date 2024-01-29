package net.spacetivity.ludo.player

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.entity.GameEntity
import net.spacetivity.ludo.utils.MetadataUtils

enum class AI_EntityPickRule(val weight: Int, val probability: Double) {

    // Weight of the rule (Int) and its corresponding probability (Double)
    // If the weights of two rules are equal, a random number from 1 to 10 is generated. If it's greater than 5, the one with the higher probability wins.
    // If the random number is less than or equal to 5, one of the two rules wins randomly.

    GARAGE_MOVABLE(0, 1.0),
    MOVABLE(1, 1.0),

    MOVABLE_AND_TARGET_IN_SIGHT(2, 0.4),
    MOVABLE_AND_GARAGE_ENTRANCE_POSSIBLE(2, 0.6),

    MOVABLE_OUT_OF_START(99, 1.0),
    NOT_MOVABLE(100, 1.0);

    companion object {

        /**
         * Analyzes the current rule situation for the AI player.
         * Determines the appropriate entity pick rule based on the game state and diced number.
         * @param aiPlayer The AI player for whom the rule situation is analyzed.
         * @param dicedNumber The number obtained from the dice roll.
         * @return The AI entity pick rule determined based on the analysis.
         */
        fun analyzeCurrentRuleSituation(aiPlayer: GamePlayer, dicedNumber: Int): AI_EntityPickRule {
            val gameEntities: List<GameEntity> = LudoGame.instance.gameEntityHandler.getEntitiesFromTeam(aiPlayer.arenaId, aiPlayer.teamName)

            var rule: AI_EntityPickRule = NOT_MOVABLE

            if (gameEntities.any { it.currentFieldId == null } && dicedNumber == 6) {
                rule = MOVABLE_OUT_OF_START
            } else if (gameEntities.all { it.currentFieldId == null } && dicedNumber == 6) {
                rule = MOVABLE_OUT_OF_START
            } else if (gameEntities.all { it.currentFieldId == null } && dicedNumber != 6) {
                rule = NOT_MOVABLE
            } else {

                val availableRules: MutableList<AI_EntityPickRule> = mutableListOf()

                for (gameEntity: GameEntity in gameEntities) {
                    if (MetadataUtils.has(gameEntity.livingEntity!!, "inGarage") && gameEntity.isMovable(dicedNumber)) {
                        availableRules.add(GARAGE_MOVABLE)
                    } else if (gameEntity.isMovable(dicedNumber)) {
                        availableRules.add(MOVABLE)
                    } else if (gameEntity.isMovable(dicedNumber) && gameEntity.hasValidTarget(dicedNumber)) {
                        availableRules.add(MOVABLE_AND_TARGET_IN_SIGHT)
                    } else if (gameEntity.isMovable(dicedNumber) && gameEntity.isGarageInSight(dicedNumber)) {
                        availableRules.add(MOVABLE_AND_GARAGE_ENTRANCE_POSSIBLE)
                    } else {
                        availableRules.add(NOT_MOVABLE)
                    }

                    // find the highest weighted rule | if two have the same weight, do the number game!
                    if (availableRules.size > 1) {
                        val rulesWithEqualWeight: List<AI_EntityPickRule> = availableRules.filter { it.weight == availableRules[0].weight }

                        if (rulesWithEqualWeight.size > 1) {
                            val randomNumber: Int = (1..10).random()

                            if (randomNumber > 5) {
                                val possibleRule: AI_EntityPickRule? = availableRules.maxByOrNull { it.probability }
                                if (possibleRule != null) rule = possibleRule
                            } else {
                                rule = rulesWithEqualWeight.random()
                            }

                        } else {
                            rule = rulesWithEqualWeight[0]
                        }

                    } else {
                        rule = availableRules[0]
                    }

                }

            }

            return rule
        }

    }

}