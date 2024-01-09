package net.spacetivity.ludo.board

import net.kyori.adventure.text.Component
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.entity.GameEntity
import net.spacetivity.ludo.team.GameTeam
import net.spacetivity.ludo.team.GameTeamHandler
import org.bukkit.Bukkit
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

    fun getPossibleHolder(): GameEntity? {
        return LudoGame.instance.gameArenaHandler.getArena(this.arenaId)?.gameEntityHandler?.gameEntities?.find { it.currentFieldId == this.id }
    }

    fun throwOut(newHolder: LivingEntity, fieldHeight: Double) {
        val gameArena: GameArena? = LudoGame.instance.gameArenaHandler.getArena(this.arenaId)

        if (!this.isTaken) return
        if (gameArena == null) return

        val gameTeamHandler: GameTeamHandler = gameArena.gameTeamHandler
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