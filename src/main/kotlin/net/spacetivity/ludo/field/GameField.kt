package net.spacetivity.ludo.field

import net.kyori.adventure.text.Component
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.entity.GameEntity
import net.spacetivity.ludo.team.GameTeam
import net.spacetivity.ludo.team.GameTeamHandler
import net.spacetivity.ludo.team.GameTeamSpawn
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.LivingEntity

class GameField(
    val id: Int,
    val arenaId: String,
    val world: World,
    val x: Double,
    val z: Double,
    var turnComponent: TurnComponent?,
    var isTaken: Boolean = false
) {

    fun throwOut(newHolder: LivingEntity, fieldHeight: Double) {
        val gameArena: GameArena? = LudoGame.instance.gameArenaHandler.getArena(this.arenaId)

        if (!this.isTaken) return
        if (gameArena == null) return

        val gameTeamHandler: GameTeamHandler = LudoGame.instance.gameTeamHandler
        val holder: GameEntity = getPossibleHolder() ?: return

        val holderGameTeam: GameTeam = gameTeamHandler.getTeam(this.arenaId, holder.teamName) ?: return
        val teamSpawnLocation: GameTeamSpawn = holderGameTeam.getFreeSpawnLocation()
            ?: throw NullPointerException("No empty team spawn was found for $holderGameTeam.name")

        holder.livingEntity?.teleport(teamSpawnLocation.getWorldPosition())
        teamSpawnLocation.isTaken = true
        newHolder.teleport(getWorldPosition(fieldHeight))

        val newHolderGameTeam: GameTeam = gameTeamHandler.getTeamOfEntity(this.arenaId, newHolder) ?: return
        gameArena.sendArenaMessage(Component.text("${newHolderGameTeam.name} has thrown out a entity from ${holderGameTeam.name}."))
    }

    fun getWorldPosition(fieldHeight: Double): Location {
        val location = Location(this.world, this.x, fieldHeight, this.z, 0.0F, 0.0F)
        val fixedLocation: Location = location.clone().toCenterLocation()
        fixedLocation.y = fieldHeight
        return fixedLocation
    }

    private fun getPossibleHolder(): GameEntity? {
        return LudoGame.instance.gameEntityHandler.getEntityAtField(arenaId, this.id)
    }

}