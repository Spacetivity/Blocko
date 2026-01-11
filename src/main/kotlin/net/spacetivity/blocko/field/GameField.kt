package net.spacetivity.blocko.field

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.achievement.grantIfCompletedBy
import net.spacetivity.blocko.achievement.impl.FirstEliminationAchievement
import net.spacetivity.blocko.achievement.impl.FirstKnockoutAchievement
import net.spacetivity.blocko.achievement.impl.MasterEliminatorAchievement
import net.spacetivity.blocko.achievement.impl.TripleThreatAchievement
import net.spacetivity.blocko.arena.id.ArenaId
import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.entity.GameEntityStatus
import net.spacetivity.blocko.field.highlighting.HighlightMode
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.stats.StatsType
import net.spacetivity.blocko.stats.addCoins
import net.spacetivity.blocko.utils.formatTeamName
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.LivingEntity

class GameField(
    val arenaId: ArenaId,
    val world: World,
    val x: Int,
    val z: Int,
    val properties: GameFieldProperties,
    var isGarageField: Boolean,
    var isTaken: Boolean = false,
) {

    var currentHighlightMode: HighlightMode? = null
    var currentHolder: GameEntity? = null

    fun trowOutOldHolder(newHolder: GamePlayer, newHolderEntity: LivingEntity) {
        val gameArena = Blocko.instance.arenaHandler.getArena(this.arenaId) ?: return

        if (!this.isTaken) return

        val gameTeamHandler = Blocko.instance.gameTeamHandler
        val oldHolderEntity = this.currentHolder ?: return

        val oldHolderGameTeam = gameTeamHandler.getTeam(this.arenaId, oldHolderEntity.teamName) ?: return

        val teamSpawnLocation = oldHolderGameTeam.getFreeSpawnLocation()
            ?: throw NullPointerException("No empty team spawn was found for $oldHolderGameTeam.name")

        oldHolderEntity.currentFieldId = null
        oldHolderEntity.livingEntity?.teleport(teamSpawnLocation.getWorldPosition())
        oldHolderEntity.entityStatus = GameEntityStatus.AT_SPAWN
        teamSpawnLocation.isTaken = true

        val newHolderGameTeam = gameTeamHandler.getTeamOfEntity(this.arenaId, newHolderEntity) ?: return

        gameArena.sendArenaMessage("blocko.main_game_loop.entity_thrown_out_by_opponent",
            Placeholder.parsed("successor_team_color", "<${newHolderGameTeam.color.asHexString()}>"),
            Placeholder.parsed("successor_team_name", newHolderGameTeam.name.formatTeamName()),
            Placeholder.parsed("victim_team_color", "<${oldHolderGameTeam.color.asHexString()}>"),
            Placeholder.parsed("victim_team_name", oldHolderGameTeam.name.formatTeamName()))

        gameArena.sendArenaSound(Sound.ENTITY_WITHER_DEATH, 0.05F)

        handleStatsReward(newHolder, true)

        val oldHolder = gameArena.currentPlayers.firstOrNull { it.teamName == oldHolderEntity.teamName } ?: return

        handleStatsReward(oldHolder, false)
    }

    fun getWorldPosition(isGameField: Boolean): Location {
        val yLevel = Blocko.instance.arenaHandler.getArena(this.arenaId)?.yLevel ?: 0.0
        val location = Location(this.world, this.x.toDouble(), yLevel, this.z.toDouble(), 0.0F, 0.0F)
        val fixedLocation = location.clone().toCenterLocation()
        fixedLocation.y = if (isGameField) yLevel - 1 else yLevel
        return fixedLocation
    }

    private fun handleStatsReward(gamePlayer: GamePlayer, isReward: Boolean) {
        if (!gamePlayer.isAI) {
            if (isReward) {
                gamePlayer.grantIfCompletedBy(FirstEliminationAchievement::class)
                gamePlayer.grantIfCompletedBy(MasterEliminatorAchievement::class)
                gamePlayer.grantIfCompletedBy(TripleThreatAchievement::class)
            } else {
                gamePlayer.grantIfCompletedBy(FirstKnockoutAchievement::class)
            }
        }

        val statsPlayer = Blocko.instance.statsPlayerHandler.getStatsPlayer(gamePlayer.uuid) ?: return
        val statsType = if (isReward) StatsType.ELIMINATED_OPPONENTS else StatsType.KNOCKED_OUT_BY_OPPONENTS
        statsPlayer.update(statsType, true, 1)

        val coinsPerElimination = Blocko.instance.globalConfigFile.coinsPerElimination

        if (isReward) {
            gamePlayer.toBukkitInstance()?.addCoins(coinsPerElimination, true)
            gamePlayer.matchStats.eliminations += 1
            gamePlayer.matchStats.gainedCoins += coinsPerElimination
        } else {
            gamePlayer.matchStats.knockedOutByOpponent += 1
        }
    }

}
