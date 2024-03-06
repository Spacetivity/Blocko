package net.spacetivity.ludo.achievement.impl

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.achievement.container.Achievement
import net.spacetivity.ludo.achievement.container.Requirement
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.player.GamePlayer

class FairPlayAchievement(translationKey: String) : Achievement(translationKey, 10, listOf(FairPlayRequirement(translationKey)))

class FairPlayRequirement(override val translationKey: String) : Requirement {
    override fun getPlaceholders(gamePlayer: GamePlayer): List<TagResolver> = emptyList()

    override fun isCompletedBy(gamePlayer: GamePlayer): Boolean {
        val gameArena: GameArena = LudoGame.instance.gameArenaHandler.getArena(gamePlayer.arenaId) ?: return false
        return gameArena.phase.isEnding()
    }
}