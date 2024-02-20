package net.spacetivity.ludo.field

import net.kyori.adventure.text.Component
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.entity.GameEntity
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

    fun trowOutOldHolder(newHolder: LivingEntity) {
        val gameArena: GameArena? = LudoGame.instance.gameArenaHandler.getArena(this.arenaId)

        if (!this.isTaken) return
        if (gameArena == null) return

        val gameTeamHandler: GameTeamHandler = LudoGame.instance.gameTeamHandler
        val oldHolder: GameEntity = getCurrentHolder() ?: return

        val holderGameTeam: GameTeam = gameTeamHandler.getTeam(this.arenaId, oldHolder.teamName) ?: return
        val teamSpawnLocation: GameTeamLocation = holderGameTeam.getFreeSpawnLocation()
            ?: throw NullPointerException("No empty team spawn was found for $holderGameTeam.name")

        oldHolder.livingEntity?.teleport(teamSpawnLocation.getWorldPosition())
        teamSpawnLocation.isTaken = true

        val newHolderGameTeam: GameTeam = gameTeamHandler.getTeamOfEntity(this.arenaId, newHolder) ?: return
        gameArena.sendArenaMessage(Component.text("${newHolderGameTeam.name} has thrown out a entity from ${holderGameTeam.name}."))
    }

    fun getWorldPosition(fieldHeight: Double): Location {
        val location = Location(this.world, this.x, fieldHeight, this.z, 0.0F, 0.0F)
        val fixedLocation: Location = location.clone().toCenterLocation()
        fixedLocation.y = fieldHeight
        return fixedLocation
    }

    fun getCurrentHolder(): GameEntity? {
        val worldPosition = getWorldPosition(0.0)
        return LudoGame.instance.gameEntityHandler.getEntityAtField(arenaId, worldPosition.x, worldPosition.z)
    }

}