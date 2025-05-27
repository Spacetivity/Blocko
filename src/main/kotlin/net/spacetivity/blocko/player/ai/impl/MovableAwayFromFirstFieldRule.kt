package net.spacetivity.blocko.player.ai.impl

import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.player.EntityPickRule
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.player.ai.AIRule
import net.spacetivity.blocko.player.ai.AIResult

class MovableAwayFromFirstFieldRule : AIRule {
    override val weight = EntityPickRule.MOVABLE_AWAY_FROM_FIRST_FIELD.weight
    override val probability = EntityPickRule.MOVABLE_AWAY_FROM_FIRST_FIELD.probability

    override fun evaluate(entity: GameEntity, player: GamePlayer, dicedNumber: Int, startField: GameField): Boolean {
        return entity.currentFieldId == 0 && entity.isMovableTo(dicedNumber)
    }

    override fun result(entity: GameEntity): AIResult {
        return AIResult(EntityPickRule.MOVABLE_AWAY_FROM_FIRST_FIELD, entity)
    }
}