package net.spacetivity.ludo.field

import net.kyori.adventure.text.Component
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.achievement.container.Achievement
import net.spacetivity.ludo.achievement.impl.FirstEliminationAchievement
import net.spacetivity.ludo.achievement.impl.FirstKnockoutAchievement
import net.spacetivity.ludo.achievement.impl.MasterEliminatorAchievement
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.entity.GameEntity
import net.spacetivity.ludo.entity.GameEntityStatus
import net.spacetivity.ludo.extensions.addCoins
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.stats.StatsPlayer
import net.spacetivity.ludo.stats.StatsType
import net.spacetivity.ludo.stats.UpdateOperation
import net.spacetivity.ludo.team.GameTeam
import net.spacetivity.ludo.team.GameTeamHandler
import net.spacetivity.ludo.team.GameTeamLocation
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
        val gameArena: GameArena = LudoGame.instance.gameArenaHandler.getArena(this.arenaId) ?: return

        if (!this.isTaken) return

        val gameTeamHandler: GameTeamHandler = LudoGame.instance.gameTeamHandler
        val oldHolderEntity: GameEntity = this.currentHolder ?: return

        val oldHolderGameTeam: GameTeam = gameTeamHandler.getTeam(this.arenaId, oldHolderEntity.teamName) ?: return

        val teamSpawnLocation: GameTeamLocation = oldHolderGameTeam.getFreeSpawnLocation()
            ?: throw NullPointerException("No empty team spawn was found for $oldHolderGameTeam.name")

        oldHolderEntity.currentFieldId = null
        oldHolderEntity.livingEntity?.teleport(teamSpawnLocation.getWorldPosition())
        oldHolderEntity.entityStatus = GameEntityStatus.AT_SPAWN
        teamSpawnLocation.isTaken = true

        val newHolderGameTeam: GameTeam = gameTeamHandler.getTeamOfEntity(this.arenaId, newHolderEntity) ?: return
        gameArena.sendArenaMessage(Component.text("${newHolderGameTeam.name} has thrown out a entity from ${oldHolderGameTeam.name}."))
        gameArena.sendArenaSound(Sound.ENTITY_WITHER_DEATH, 0.05F)

        handleStatsReward(newHolder, true)

        val oldHolder: GamePlayer = gameArena.currentPlayers.firstOrNull { it.teamName == oldHolderEntity.teamName }
            ?: return

        handleStatsReward(oldHolder, false)
    }

    fun getWorldPosition(fieldHeight: Double): Location {
        val location = Location(this.world, this.x, fieldHeight, this.z, 0.0F, 0.0F)
        val fixedLocation: Location = location.clone().toCenterLocation()
        fixedLocation.y = fieldHeight
        return fixedLocation
    }

    private fun handleStatsReward(gamePlayer: GamePlayer, isReward: Boolean) {
        if (!gamePlayer.isAI) {
            val possibleAchievements: MutableSet<Achievement?> = mutableSetOf()

            if (isReward) {
                possibleAchievements.add(LudoGame.instance.achievementHandler.getAchievement(FirstEliminationAchievement::class.java))
                possibleAchievements.add(LudoGame.instance.achievementHandler.getAchievement(MasterEliminatorAchievement::class.java))
            } else {
                possibleAchievements.add(LudoGame.instance.achievementHandler.getAchievement(FirstKnockoutAchievement::class.java))
            }

            possibleAchievements.forEach { it?.grantIfCompletedBy(gamePlayer) }
        }

        val statsPlayer: StatsPlayer = LudoGame.instance.statsPlayerHandler.getStatsPlayer(gamePlayer.uuid) ?: return
        val statsType: StatsType = if (isReward) StatsType.ELIMINATED_OPPONENTS else StatsType.KNOCKED_OUT_BY_OPPONENTS
        statsPlayer.update(statsType, UpdateOperation.INCREASE, 1)

        val coinsPerKill = 20

        if (isReward) {
            gamePlayer.toBukkitInstance()?.addCoins(coinsPerKill, true)
            gamePlayer.matchStats.eliminations += 1
            gamePlayer.matchStats.gainedCoins += coinsPerKill
        } else {
            gamePlayer.matchStats.knockedOutByOpponent += 1
        }
    }

}
