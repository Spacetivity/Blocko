package net.spacetivity.blocko.player.ai.impl

import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.player.EntityPickRule
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.player.ai.AIRule
import net.spacetivity.blocko.player.ai.AIResult

class MovableButLandsAfterOpponentRule : AIRule {
    override val weight = EntityPickRule.MOVABLE_BUT_LANDS_AFTER_OPPONENT.weight
    override val probability = EntityPickRule.MOVABLE_BUT_LANDS_AFTER_OPPONENT.probability

    override fun evaluate(entity: GameEntity, player: GamePlayer, dicedNumber: Int, startField: GameField): Boolean {
        return entity.isMovableTo(dicedNumber) && entity.landsAfterOpponent(dicedNumber)
    }

    override fun result(entity: GameEntity): AIResult {
        return AIResult(EntityPickRule.MOVABLE_BUT_LANDS_AFTER_OPPONENT, entity)
    }
}