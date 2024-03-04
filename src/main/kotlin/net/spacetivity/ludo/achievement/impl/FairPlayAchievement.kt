package net.spacetivity.ludo.achievement.impl

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.achievement.container.Achievement
import net.spacetivity.ludo.achievement.container.Requirement
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.player.GamePlayer

class FairPlayAchievement : Achievement("FairPlay", "Write gg at the end of a game!", listOf(FairPlayRequirement()))

class FairPlayRequirement : Requirement {
    override fun isCompletedBy(gamePlayer: GamePlayer): Boolean {
        val gameArena: GameArena = LudoGame.instance.gameArenaHandler.getArena(gamePlayer.arenaId) ?: return false
        return gameArena.phase.isEnding()
    }
}