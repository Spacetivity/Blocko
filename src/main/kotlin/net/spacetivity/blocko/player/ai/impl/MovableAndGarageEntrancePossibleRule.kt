package net.spacetivity.blocko.player.ai.impl

import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.player.EntityPickRule
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.player.ai.AiRule

class MovableAndGarageEntrancePossibleRule : AiRule {
    override val weight: Int = EntityPickRule.MOVABLE_AND_GARAGE_ENTRANCE_POSSIBLE.weight
    override val probability: Double = EntityPickRule.MOVABLE_AND_GARAGE_ENTRANCE_POSSIBLE.probability

    override fun evaluate(entity: GameEntity, player: GamePlayer, dicedNumber: Int, startField: GameField): Boolean {
        return entity.isMovableTo(dicedNumber) && entity.isGarageInSight(dicedNumber)
    }

    override fun result(entity: GameEntity): Pair<EntityPickRule, GameEntity?> {
        return Pair(EntityPickRule.MOVABLE_AND_GARAGE_ENTRANCE_POSSIBLE, entity)
    }
}