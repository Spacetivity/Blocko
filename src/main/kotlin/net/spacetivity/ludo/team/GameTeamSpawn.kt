package net.spacetivity.ludo.team

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World

data class GameTeamSpawn(
    val arenaId: String,
    val teamName: String,
    val worldName: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
    var isTaken: Boolean
) {

    fun getWorldPosition(): Location {
        val world: World = Bukkit.getWorld(this.worldName)!!
        val location = Location(world, this.x, this.y, this.z, this.yaw, this.pitch)
        val fixedLocation: Location = location.clone().toCenterLocation()
        fixedLocation.y = this.y
        return fixedLocation
    }

}