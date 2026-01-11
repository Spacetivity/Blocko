package net.spacetivity.blocko.player.ai

import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.player.EntityPickRule

data class AIResult(val rule: EntityPickRule, val selectedEntity: GameEntity?)