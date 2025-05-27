package net.spacetivity.blocko.player.ai.impl

import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.player.EntityPickRule
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.player.ai.AIRule
import net.spacetivity.blocko.player.ai.AIResult

class MovableOutOfStartRule : AIRule {
    override val weight = EntityPickRule.MOVABLE_OUT_OF_START.weight
    override val probability = EntityPickRule.MOVABLE_OUT_OF_START.probability

    override fun evaluate(entity: GameEntity, player: GamePlayer, dicedNumber: Int, startField: GameField): Boolean {
        return entity.isAtSpawn() &&
                dicedNumber == 6 &&
                (startField.currentHolder == null || startField.currentHolder?.teamName != player.teamName)
    }

    override fun result(entity: GameEntity): AIResult {
        return AIResult(EntityPickRule.MOVABLE_OUT_OF_START, entity)
    }
}