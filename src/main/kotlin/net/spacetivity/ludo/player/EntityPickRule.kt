package net.spacetivity.ludo.player

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.entity.GameEntity
import net.spacetivity.ludo.field.GameField

enum class EntityPickRule(val weight: Int, val probability: Double) {

    // Weight of the rule (Int) and its corresponding probability (Double)
    // If the weights of two rules are equal, a random number from 1 to 10 is generated. If it's greater than 5, the one with the higher probability wins.
    // If the random number is less than or equal to 5, one of the two rules wins randomly.

//    MOVABLE(1, 0.6),
//    MOVABLE_BUT_LANDS_AFTER_OPPONENT(1, 0.4),
//
//    MOVABLE_AND_TARGET_IN_SIGHT(2, 0.4),
//    MOVABLE_AND_GARAGE_ENTRANCE_POSSIBLE(2, 0.6),
//
//    MOVABLE_OUT_OF_START(98, 1.0),
//    MOVABLE_AWAY_FROM_FIRST_FIELD(99, 1.0),
//    NOT_MOVABLE(100, 1.0);


    MOVABLE_OUT_OF_START(5, 1.0),

    MOVABLE_AWAY_FROM_FIRST_FIELD(4, 1.0),

    MOVABLE_AND_GARAGE_ENTRANCE_POSSIBLE(3, 0.6),

    MOVABLE_AND_TARGET_IN_SIGHT(3, 0.4),

    MOVABLE(2, 0.6),

    MOVABLE_BUT_LANDS_AFTER_OPPONENT(1, 0.4),

    NOT_MOVABLE(0, 1.0);

    companion object {

        fun analyzeCurrentRuleSituation(gamePlayer: GamePlayer, dicedNumber: Int): Pair<EntityPickRule, GameEntity?> {
            val gameEntities: List<GameEntity> = LudoGame.instance.gameEntityHandler.getEntitiesFromTeam(gamePlayer.arenaId, gamePlayer.teamName)
            var bestRule: Pair<EntityPickRule, GameEntity?> = Pair(EntityPickRule.NOT_MOVABLE, null)

            val startFieldForTeam: GameField = LudoGame.instance.gameFieldHandler.getFirstFieldForTeam(gamePlayer.arenaId, gamePlayer.teamName)!!

            for (gameEntity: GameEntity in gameEntities) {
                val rule: Pair<EntityPickRule, GameEntity?> = when {
                    gameEntity.isAtSpawn() && dicedNumber == 6 && (startFieldForTeam.currentHolder == null || startFieldForTeam.currentHolder?.teamName != gamePlayer.teamName) -> {
                        Pair(EntityPickRule.MOVABLE_OUT_OF_START, gameEntity)
                    }

                    gameEntity.currentFieldId == 0 && gameEntity.isMovableTo(dicedNumber) -> {
                        println("!!!<==>>>>>>> REACHED AWAY FROM FIRST FIELD <<<<<<<==>!!!")
                        Pair(EntityPickRule.MOVABLE_AWAY_FROM_FIRST_FIELD, gameEntity)
                    }

                    gameEntity.isMovableTo(dicedNumber) && gameEntity.landsAfterOpponent(dicedNumber) -> {
                        println("!!!<==>>>>>>> REACHED MOVABLE_BUT_LANDS_AFTER_OPPONENT <<<<<<<==>!!!")
                        Pair(EntityPickRule.MOVABLE_BUT_LANDS_AFTER_OPPONENT, gameEntity)
                    }

                    gameEntity.isMovableTo(dicedNumber) && gameEntity.hasTargetAtGoalField(dicedNumber) -> {
                        println("!!!<==>>>>>>> REACHED MOVABLE_AND_TARGET_IN_SIGHT <<<<<<<==>!!!")
                        Pair(EntityPickRule.MOVABLE_AND_TARGET_IN_SIGHT, gameEntity)
                    }

                    gameEntity.isMovableTo(dicedNumber) && gameEntity.isGarageInSight(dicedNumber) -> {
                        println("!!!<==>>>>>>> REACHED MOVABLE_AND_GARAGE_ENTRANCE_POSSIBLE <<<<<<<==>!!!")
                        Pair(EntityPickRule.MOVABLE_AND_GARAGE_ENTRANCE_POSSIBLE, gameEntity)
                    }

                    gameEntity.isMovableTo(dicedNumber) -> {
                        println("!!!<==>>>>>>> REACHED MOVABLE <<<<<<<==>!!!")
                        Pair(EntityPickRule.MOVABLE, gameEntity)
                    }

                    else -> {
                        println("!!!<==>>>>>>> REACHED NOT_MOVABLE <<<<<<<==>!!!")
                        Pair(EntityPickRule.NOT_MOVABLE, null)
                    }
                }

                if (bestRule.first.weight < rule.first.weight || (bestRule.first.weight == rule.first.weight && (1..10).random() > 5 && bestRule.first.probability < rule.first.probability)) {
                    bestRule = rule
                }

//                if (bestRule.first.weight < rule.first.weight) {
//                    bestRule = rule;
//                } else if (bestRule.first.weight == rule.first.weight) {
//                    // Bei gleicher Gewichtung kann die Wahrscheinlichkeit oder ein anderer Faktor als Tie-Breaker dienen
//                    if (bestRule.first.probability < rule.first.probability) {
//                        bestRule = rule;
//                    }
//                }

            }

            // Fallback für spezielle Fälle außerhalb der Schleife
            if (gameEntities.all { it.isAtSpawn() }) {
                if (dicedNumber == 6) {
                    return Pair(EntityPickRule.MOVABLE_OUT_OF_START, gameEntities.random())
                } else {
                    return Pair(EntityPickRule.NOT_MOVABLE, null)
                }
            }

            return bestRule
        }

    }

}