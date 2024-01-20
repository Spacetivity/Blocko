package net.spacetivity.ludo.field

import net.kyori.adventure.text.Component
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.entity.GameEntity
import net.spacetivity.ludo.team.GameTeam
import net.spacetivity.ludo.team.GameTeamHandler
import net.spacetivity.ludo.team.GameTeamLocation
import net.spacetivity.ludo.utils.BoardField
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.LivingEntity

class GameField(
    override val id: Int,
    override val arenaId: String,
    override val world: World,
    override val x: Double,
    override val z: Double,
    var turnComponent: TurnComponent?,
    var teamGarageEntrance: String?,
    override var isTaken: Boolean = false
) : BoardField {

    fun checkForOpponent(newHolder: LivingEntity) {
        val gameArena: GameArena? = LudoGame.instance.gameArenaHandler.getArena(this.arenaId)

        if (!this.isTaken) return
        if (gameArena == null) return

        val gameTeamHandler: GameTeamHandler = LudoGame.instance.gameTeamHandler
        val holder: GameEntity = getCurrentHolder() ?: return

        if (holder.livingEntity!!.uniqueId == newHolder.uniqueId) return

        val holderGameTeam: GameTeam = gameTeamHandler.getTeam(this.arenaId, holder.teamName) ?: return
        val teamSpawnLocation: GameTeamLocation = holderGameTeam.getFreeSpawnLocation()
            ?: throw NullPointerException("No empty team spawn was found for $holderGameTeam.name")

        holder.livingEntity?.teleport(teamSpawnLocation.getWorldPosition())
        teamSpawnLocation.isTaken = true

        val newHolderGameTeam: GameTeam = gameTeamHandler.getTeamOfEntity(this.arenaId, newHolder) ?: return
        gameArena.sendArenaMessage(Component.text("${newHolderGameTeam.name} has thrown out a entity from ${holderGameTeam.name}."))
    }

    override fun getWorldPosition(fieldHeight: Double): Location {
        val location = Location(this.world, this.x, fieldHeight, this.z, 0.0F, 0.0F)
        val fixedLocation: Location = location.clone().toCenterLocation()
        fixedLocation.y = fieldHeight
        return fixedLocation
    }

    override fun getCurrentHolder(): GameEntity? {
        return LudoGame.instance.gameEntityHandler.getEntityAtField(arenaId, this.id)
    }

}