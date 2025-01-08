package net.spacetivity.blocko.achievement.impl

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.achievement.container.Achievement
import net.spacetivity.blocko.achievement.container.Requirement
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.player.GamePlayer

class BadMannersAchievement(translationKey: String) : Achievement(translationKey, 5, listOf(BadMannersRequirement(translationKey)))

class BadMannersRequirement(override val translationKey: String) : Requirement {
    override fun getPlaceholders(gamePlayer: GamePlayer): List<TagResolver> = emptyList()

    override fun isCompletedBy(gamePlayer: GamePlayer): Boolean {
        val gameArena: GameArena = BlockoGame.instance.gameArenaHandler.getArena(gamePlayer.arenaId) ?: return false
        return gameArena.phase.isEnding()
    }
}
