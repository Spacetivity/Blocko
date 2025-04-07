package net.spacetivity.blocko.player.ai.impl

import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.player.EntityPickRule
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.player.ai.AiRule

class MovableAwayFromFirstFieldRule : AiRule {
    override val weight: Int = EntityPickRule.MOVABLE_AWAY_FROM_FIRST_FIELD.weight
    override val probability: Double = EntityPickRule.MOVABLE_AWAY_FROM_FIRST_FIELD.probability

    override fun evaluate(entity: GameEntity, player: GamePlayer, dicedNumber: Int, startField: GameField): Boolean {
        return entity.currentFieldId == 0 && entity.isMovableTo(dicedNumber)
    }

    override fun result(entity: GameEntity): Pair<EntityPickRule, GameEntity?> {
        return Pair(EntityPickRule.MOVABLE_AWAY_FROM_FIRST_FIELD, entity)
    }
}