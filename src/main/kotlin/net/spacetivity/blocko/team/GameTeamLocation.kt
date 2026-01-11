package net.spacetivity.blocko.team

import net.spacetivity.blocko.arena.id.ArenaId
import org.bukkit.Bukkit
import org.bukkit.Location

data class GameTeamLocation(
    val arenaId: ArenaId,
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
        val world = Bukkit.getWorld(this.worldName)!!
        val location = Location(world, this.x, this.y, this.z, this.yaw, this.pitch)
        val fixedLocation = location.clone().toCenterLocation()
        fixedLocation.y = this.y
        return fixedLocation
    }

}