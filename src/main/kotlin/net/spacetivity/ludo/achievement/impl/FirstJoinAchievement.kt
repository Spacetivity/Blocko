package net.spacetivity.ludo.achievement.impl

import net.spacetivity.ludo.achievement.container.Achievement
import net.spacetivity.ludo.achievement.container.Requirement
import net.spacetivity.ludo.player.GamePlayer

class FirstJoinAchievement : Achievement("FirstJoin", "Join the game server for the first time!", listOf(FirstJoinRequirement()))

class FirstJoinRequirement : Requirement {
    override fun isCompletedBy(gamePlayer: GamePlayer): Boolean {
        return true //TODO: So ändern, dass es testet, ob man bereits eine Game Runde gespielt hat! :O (gamePlayer.getStats == null)
    }
}