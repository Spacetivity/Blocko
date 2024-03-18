package net.spacetivity.blocko.player

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.field.GameField

enum class EntityPickRule(val weight: Int, val probability: Double) {

    // Blocko-Bots - Logic by Tobias Heimböck (TGamings)
    // Weight of the rule (Int) and its corresponding probability (Double)
    // If the weights of two rules are equal, a random number from 1 to 10 is generated. If it's greater than 5, the one with the higher probability wins.
    // If the random number is less than or equal to 5, one of the two rules wins randomly.

    MOVABLE_OUT_OF_START(5, 1.0),
    MOVABLE_AWAY_FROM_FIRST_FIELD(4, 1.0),
    MOVABLE_AND_GARAGE_ENTRANCE_POSSIBLE(3, 0.6),
    MOVABLE_AND_TARGET_IN_SIGHT(3, 0.4),
    MOVABLE(2, 0.6),
    MOVABLE_BUT_LANDS_AFTER_OPPONENT(1, 0.4),
    NOT_MOVABLE(0, 1.0);

    companion object {

        fun analyzeCurrentRuleSituation(gamePlayer: GamePlayer, dicedNumber: Int): Pair<EntityPickRule, GameEntity?> {
            val gameEntities: List<GameEntity> = BlockoGame.instance.gameEntityHandler.getEntitiesFromTeam(gamePlayer.arenaId, gamePlayer.teamName!!)
            var bestRule: Pair<EntityPickRule, GameEntity?> = Pair(NOT_MOVABLE, null)

            val startFieldForTeam: GameField = BlockoGame.instance.gameFieldHandler.getFirstFieldForTeam(gamePlayer.arenaId, gamePlayer.teamName!!)!!

            for (gameEntity: GameEntity in gameEntities) {
                val rule: Pair<EntityPickRule, GameEntity?> = when {
                    gameEntity.isAtSpawn() && dicedNumber == 6 && (startFieldForTeam.currentHolder == null || startFieldForTeam.currentHolder?.teamName != gamePlayer.teamName) -> Pair(MOVABLE_OUT_OF_START, gameEntity)
                    gameEntity.currentFieldId == 0 && gameEntity.isMovableTo(dicedNumber) -> Pair(MOVABLE_AWAY_FROM_FIRST_FIELD, gameEntity)
                    gameEntity.isMovableTo(dicedNumber) && gameEntity.landsAfterOpponent(dicedNumber) -> Pair(MOVABLE_BUT_LANDS_AFTER_OPPONENT, gameEntity)
                    gameEntity.isMovableTo(dicedNumber) && gameEntity.hasTargetAtGoalField(dicedNumber) -> Pair(MOVABLE_AND_TARGET_IN_SIGHT, gameEntity)
                    gameEntity.isMovableTo(dicedNumber) && gameEntity.isGarageInSight(dicedNumber) -> Pair(MOVABLE_AND_GARAGE_ENTRANCE_POSSIBLE, gameEntity)
                    gameEntity.isMovableTo(dicedNumber) -> Pair(MOVABLE, gameEntity)
                    else -> Pair(NOT_MOVABLE, null)
                }

                if (bestRule.first.weight < rule.first.weight || (bestRule.first.weight == rule.first.weight && (1..10).random() > 5 && bestRule.first.probability < rule.first.probability)) {
                    bestRule = rule
                }
            }

            if (gameEntities.all { it.isAtSpawn() }) {
                if (dicedNumber == 6) {
                    return Pair(MOVABLE_OUT_OF_START, gameEntities.random())
                } else {
                    return Pair(NOT_MOVABLE, null)
                }
            }

            return bestRule
        }

    }

}