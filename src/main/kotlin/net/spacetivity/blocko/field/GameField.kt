package net.spacetivity.blocko.field

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.achievement.container.Achievement
import net.spacetivity.blocko.achievement.impl.FirstEliminationAchievement
import net.spacetivity.blocko.achievement.impl.FirstKnockoutAchievement
import net.spacetivity.blocko.achievement.impl.MasterEliminatorAchievement
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.entity.GameEntityStatus
import net.spacetivity.blocko.extensions.addCoins
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.stats.StatsPlayer
import net.spacetivity.blocko.stats.StatsType
import net.spacetivity.blocko.stats.UpdateOperation
import net.spacetivity.blocko.team.GameTeam
import net.spacetivity.blocko.team.GameTeamHandler
import net.spacetivity.blocko.team.GameTeamLocation
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.LivingEntity

class GameField(
    val arenaId: String,
    val world: World,
    val x: Double,
    val z: Double,
    val properties: GameFieldProperties,
    var isGarageField: Boolean,
    var isTaken: Boolean = false
) {

    var currentHolder: GameEntity? = null

    fun trowOutOldHolder(newHolder: GamePlayer, newHolderEntity: LivingEntity) {
        val gameArena: GameArena = BlockoGame.instance.gameArenaHandler.getArena(this.arenaId) ?: return

        if (!this.isTaken) return

        val gameTeamHandler: GameTeamHandler = BlockoGame.instance.gameTeamHandler
        val oldHolderEntity: GameEntity = this.currentHolder ?: return

        val oldHolderGameTeam: GameTeam = gameTeamHandler.getTeam(this.arenaId, oldHolderEntity.teamName) ?: return

        val teamSpawnLocation: GameTeamLocation = oldHolderGameTeam.getFreeSpawnLocation()
            ?: throw NullPointerException("No empty team spawn was found for $oldHolderGameTeam.name")

        oldHolderEntity.currentFieldId = null
        oldHolderEntity.livingEntity?.teleport(teamSpawnLocation.getWorldPosition())
        oldHolderEntity.entityStatus = GameEntityStatus.AT_SPAWN
        teamSpawnLocation.isTaken = true

        val newHolderGameTeam: GameTeam = gameTeamHandler.getTeamOfEntity(this.arenaId, newHolderEntity) ?: return

        gameArena.sendArenaMessage("blocko.main_game_loop.entity_thrown_out_by_opponent",
            Placeholder.parsed("successor_team_color", "<${newHolderGameTeam.color.asHexString()}>"),
            Placeholder.parsed("successor_team_name", newHolderGameTeam.name.lowercase().replaceFirstChar { it.uppercase() }),
            Placeholder.parsed("victim_team_color", "<${oldHolderGameTeam.color.asHexString()}>"),
            Placeholder.parsed("victim_team_name", oldHolderGameTeam.name.lowercase().replaceFirstChar { it.uppercase() }))

        gameArena.sendArenaSound(Sound.ENTITY_WITHER_DEATH, 0.05F)

        handleStatsReward(newHolder, true)

        val oldHolder: GamePlayer = gameArena.currentPlayers.firstOrNull { it.teamName == oldHolderEntity.teamName }
            ?: return

        handleStatsReward(oldHolder, false)
    }

    fun getWorldPosition(): Location {
        val yLevel: Double = BlockoGame.instance.gameArenaHandler.getArena(this.arenaId)?.yLevel ?: 0.0
        val location = Location(this.world, this.x, yLevel, this.z, 0.0F, 0.0F)
        val fixedLocation: Location = location.clone().toCenterLocation()
        fixedLocation.y = yLevel
        return fixedLocation
    }

    private fun handleStatsReward(gamePlayer: GamePlayer, isReward: Boolean) {
        if (!gamePlayer.isAI) {
            val possibleAchievements: MutableSet<Achievement?> = mutableSetOf()

            if (isReward) {
                possibleAchievements.add(BlockoGame.instance.achievementHandler.getAchievement(FirstEliminationAchievement::class.java))
                possibleAchievements.add(BlockoGame.instance.achievementHandler.getAchievement(MasterEliminatorAchievement::class.java))
            } else {
                possibleAchievements.add(BlockoGame.instance.achievementHandler.getAchievement(FirstKnockoutAchievement::class.java))
            }

            possibleAchievements.forEach { it?.grantIfCompletedBy(gamePlayer) }
        }

        val statsPlayer: StatsPlayer = BlockoGame.instance.statsPlayerHandler.getStatsPlayer(gamePlayer.uuid) ?: return
        val statsType: StatsType = if (isReward) StatsType.ELIMINATED_OPPONENTS else StatsType.KNOCKED_OUT_BY_OPPONENTS
        statsPlayer.update(statsType, UpdateOperation.INCREASE, 1)

        val coinsPerElimination: Int = BlockoGame.instance.globalConfigFile.coinsPerElimination

        if (isReward) {
            gamePlayer.toBukkitInstance()?.addCoins(coinsPerElimination, true)
            gamePlayer.matchStats.eliminations += 1
            gamePlayer.matchStats.gainedCoins += coinsPerElimination
        } else {
            gamePlayer.matchStats.knockedOutByOpponent += 1
        }
    }

}
