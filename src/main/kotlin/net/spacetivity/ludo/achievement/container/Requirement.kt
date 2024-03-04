package net.spacetivity.ludo.achievement.container

import net.spacetivity.ludo.player.GamePlayer

interface Requirement {

    fun isCompletedBy(gamePlayer: GamePlayer): Boolean

}