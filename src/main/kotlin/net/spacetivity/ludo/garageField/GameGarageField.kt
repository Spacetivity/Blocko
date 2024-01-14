package net.spacetivity.ludo.garageField

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.entity.GameEntity
import net.spacetivity.ludo.utils.BoardField
import org.bukkit.Location
import org.bukkit.World

class GameGarageField(
    override val id: Int,
    override val arenaId: String,
    val teamName: String,
    override val world: World,
    override val x: Double,
    override val z: Double,
    override var isTaken: Boolean = false
) : BoardField {

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