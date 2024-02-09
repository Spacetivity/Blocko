package net.spacetivity.ludo.player

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.entity.GameEntity
import net.spacetivity.ludo.utils.MetadataUtils

enum class AI_EntityPickRule(val weight: Int, val probability: Double) {

    // Weight of the rule (Int) and its corresponding probability (Double)
    // If the weights of two rules are equal, a random number from 1 to 10 is generated. If it's greater than 5, the one with the higher probability wins.
    // If the random number is less than or equal to 5, one of the two rules wins randomly.

    GARAGE_MOVABLE(0, 1.0),

    MOVABLE(1, 0.6),
    MOVABLE_BUT_LANDS_AFTER_OPPONENT(1, 0.4),

    MOVABLE_AND_TARGET_IN_SIGHT(2, 0.4),
    MOVABLE_AND_GARAGE_ENTRANCE_POSSIBLE(2, 0.6),

    MOVABLE_OUT_OF_START(99, 1.0),
    NOT_MOVABLE(100, 1.0);

    companion object {

        fun analyzeCurrentRuleSituation(aiPlayer: GamePlayer, dicedNumber: Int): Pair<AI_EntityPickRule, GameEntity?> {
            val gameEntities: List<GameEntity> = LudoGame.instance.gameEntityHandler.getEntitiesFromTeam(aiPlayer.arenaId, aiPlayer.teamName)

            var rule: Pair<AI_EntityPickRule, GameEntity?> = Pair(NOT_MOVABLE, null)

            if (gameEntities.any { it.currentFieldId == null } && dicedNumber == 6) {
                rule = Pair(MOVABLE_OUT_OF_START, gameEntities.random())
            } else if (gameEntities.all { it.currentFieldId == null } && dicedNumber == 6) {
                rule = Pair(MOVABLE_OUT_OF_START, gameEntities.random())
            } else if (gameEntities.all { it.currentFieldId == null } && dicedNumber != 6) {
                rule = Pair(NOT_MOVABLE, null)
            } else {

                val availableRules: MutableList<Pair<AI_EntityPickRule, GameEntity?>> = mutableListOf()

                for (gameEntity: GameEntity in gameEntities) {
                    if (MetadataUtils.has(gameEntity.livingEntity!!, "inGarage") && gameEntity.isMovable(dicedNumber)) {
                        availableRules.add(Pair(GARAGE_MOVABLE, gameEntity))
                    } else if (gameEntity.isMovable(dicedNumber)) {
                        availableRules.add(Pair(MOVABLE, gameEntity))
                    } else if (gameEntity.isMovable(dicedNumber) && gameEntity.landsAfterOpponent(dicedNumber)) {
                        availableRules.add(Pair(MOVABLE_BUT_LANDS_AFTER_OPPONENT, gameEntity))
                    } else if (gameEntity.isMovable(dicedNumber) && gameEntity.hasValidTarget(dicedNumber)) {
                        availableRules.add(Pair(MOVABLE_AND_TARGET_IN_SIGHT, gameEntity))
                    } else if (gameEntity.isMovable(dicedNumber) && gameEntity.isGarageInSight(dicedNumber)) {
                        availableRules.add(Pair(MOVABLE_AND_GARAGE_ENTRANCE_POSSIBLE, gameEntity))
                    } else {
                        rule = Pair(NOT_MOVABLE, null)
                    }

                    // find the highest weighted rule | if two have the same weight, do the number game!
                    if (availableRules.size > 1) {
                        val rulesWithEqualWeight: List<Pair<AI_EntityPickRule, GameEntity?>> = availableRules.filter { it.first.weight == availableRules[0].first.weight }

                        if (rulesWithEqualWeight.size > 1) {
                            val randomNumber: Int = (1..10).random()

                            if (randomNumber > 5) {
                                val possibleRule: Pair<AI_EntityPickRule, GameEntity?>? = availableRules.maxByOrNull { it.first.probability }
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