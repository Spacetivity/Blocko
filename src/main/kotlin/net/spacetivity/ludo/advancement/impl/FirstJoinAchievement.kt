package net.spacetivity.ludo.advancement.impl

import net.spacetivity.ludo.advancement.container.Achievement
import net.spacetivity.ludo.advancement.container.Requirement
import net.spacetivity.ludo.player.GamePlayer
import org.bukkit.Material

class FirstJoinAchievement : Achievement("FirstJoin", "Join the game server for the first time!", Material.DARK_OAK_DOOR, listOf(FirstJoinRequirement()))

class FirstJoinRequirement : Requirement {
    override fun isCompletedBy(gamePlayer: GamePlayer): Boolean {
        return true //TODO: So ändern, dass es testet, ob man bereits eine Game Runde gespielt hat! :O
    }
}