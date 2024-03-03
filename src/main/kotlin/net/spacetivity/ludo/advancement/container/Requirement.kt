package net.spacetivity.ludo.advancement.container

import net.spacetivity.ludo.player.GamePlayer

interface Requirement {

    fun isCompletedBy(gamePlayer: GamePlayer): Boolean

}