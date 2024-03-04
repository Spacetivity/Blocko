package net.spacetivity.ludo.field

import net.kyori.adventure.text.Component
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.entity.GameEntity
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.stats.StatsPlayer
import net.spacetivity.ludo.stats.UpdateOperator
import net.spacetivity.ludo.stats.UpdateType
import net.spacetivity.ludo.team.GameTeam
import net.spacetivity.ludo.team.GameTeamHandler
import net.spacetivity.ludo.team.GameTeamLocation
import org.bukkit.Location
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
        teamSpawnLocation.isTaken = true

        val newHolderGameTeam: GameTeam = gameTeamHandler.getTeamOfEntity(this.arenaId, newHolderEntity) ?: return
        gameArena.sendArenaMessage(Component.text("${newHolderGameTeam.name} has thrown out a entity from ${oldHolderGameTeam.name}."))

        handleStatsReward(newHolder, true)

        val oldHolder: GamePlayer = gameArena.currentPlayers.firstOrNull { it.teamName == oldHolderEntity.teamName } ?: return
        handleStatsReward(oldHolder, false)
    }

    private fun handleStatsReward(gamePlayer: GamePlayer, isReward: Boolean) {
        val statsPlayer: StatsPlayer = LudoGame.instance.statsPlayerHandler.getCachedStatsPlayer(gamePlayer.uuid) ?: return
        val updateType: UpdateType = if (isReward) UpdateType.ELIMINATED_OPPONENTS else UpdateType.KNOCKED_OUT_BY_OPPONENTS
        statsPlayer.update(updateType, UpdateOperator.INCREASE, 1)
    }

    fun getWorldPosition(fieldHeight: Double): Location {
        val location = Location(this.world, this.x, fieldHeight, this.z, 0.0F, 0.0F)
        val fixedLocation: Location = location.clone().toCenterLocation()
        fixedLocation.y = fieldHeight
        return fixedLocation
    }

}