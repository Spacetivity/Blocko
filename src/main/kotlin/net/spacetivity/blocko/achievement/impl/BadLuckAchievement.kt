package net.spacetivity.blocko.achievement.impl

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.achievement.container.Achievement
import net.spacetivity.blocko.achievement.container.Requirement
import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.player.GamePlayer

class BadLuckAchievement(translationKey: String) : Achievement(translationKey, 10, listOf(BadLuckRequirement(translationKey)))

class BadLuckRequirement(override val translationKey: String) : Requirement {
    override fun getPlaceholders(gamePlayer: GamePlayer): List<TagResolver> = emptyList()

    override fun isCompletedBy(gamePlayer: GamePlayer): Boolean {
        val teamName: String = gamePlayer.teamName ?: return false
        val teamEntities: List<GameEntity> = BlockoGame.instance.gameEntityHandler.getEntitiesFromTeam(gamePlayer.arenaId, teamName)
        return teamEntities.none { it.isInGarage() }
    }

}
