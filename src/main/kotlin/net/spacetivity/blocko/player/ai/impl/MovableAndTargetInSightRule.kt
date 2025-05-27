package net.spacetivity.blocko.player.ai.impl

import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.player.EntityPickRule
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.player.ai.AIRule
import net.spacetivity.blocko.player.ai.AIResult

class MovableAndTargetInSightRule : AIRule {
    override val weight = EntityPickRule.MOVABLE_AND_TARGET_IN_SIGHT.weight
    override val probability = EntityPickRule.MOVABLE_AND_TARGET_IN_SIGHT.probability

    override fun evaluate(entity: GameEntity, player: GamePlayer, dicedNumber: Int, startField: GameField): Boolean {
        return entity.isMovableTo(dicedNumber) && entity.hasTargetAtGoalField(dicedNumber)
    }

    override fun result(entity: GameEntity): AIResult {
        return AIResult(EntityPickRule.MOVABLE_AND_TARGET_IN_SIGHT, entity)
    }
}