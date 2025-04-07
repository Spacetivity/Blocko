package net.spacetivity.blocko.player.ai

import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.player.EntityPickRule
import net.spacetivity.blocko.player.GamePlayer

interface AiRule {
    val weight: Int
    val probability: Double

    fun evaluate(entity: GameEntity, player: GamePlayer, dicedNumber: Int, startField: GameField): Boolean
    fun result(entity: GameEntity): Pair<EntityPickRule, GameEntity?>
}