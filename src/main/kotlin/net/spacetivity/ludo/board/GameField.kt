package net.spacetivity.ludo.board

import net.kyori.adventure.text.Component
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.entity.GameEntity
import net.spacetivity.ludo.team.GameTeam
import net.spacetivity.ludo.team.GameTeamHandler
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.LivingEntity

class GameField(
    val world: World,
    val id: Int,
    val x: Double,
    val z: Double,
    var isTaken: Boolean = false
) {

    fun getPossibleHolder(): GameEntity? {
        return LudoGame.instance.gameEntityHandler.gameEntities.find { it.currentFieldId == this.id }
    }

    fun throwOut(newHolder: LivingEntity, fieldHeight: Double) {
        if (!this.isTaken) return

        val gameTeamHandler: GameTeamHandler = LudoGame.instance.gameTeamHandler
        val holder: GameEntity = getPossibleHolder() ?: return

        val holderGameTeam: GameTeam = gameTeamHandler.getTeam(holder.teamName) ?: return
        val teamSpawnLocation: Location = holderGameTeam.spawnLocation ?: return

        holder.livingEntity?.teleport(teamSpawnLocation)
        newHolder.teleport(getWorldPosition(fieldHeight))

        val newHolderGameTeam: GameTeam = gameTeamHandler.getTeamOfEntity(newHolder) ?: return
        Bukkit.broadcast(Component.text("${newHolderGameTeam.name} has thrown out a entity from ${holderGameTeam.name}."))
    }

    fun getWorldPosition(fieldHeight: Double): Location = Location(this.world, this.x, fieldHeight, this.z, 0.0F, 0.0F)

}