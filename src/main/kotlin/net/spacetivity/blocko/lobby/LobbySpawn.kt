package net.spacetivity.blocko.lobby

import org.bukkit.Bukkit
import org.bukkit.Location

data class LobbySpawn(val worldName: String, val x: Double, val y: Double, val z: Double, val yaw: Float, val pitch: Float) {

    fun toBukkitInstance() = Location(Bukkit.getWorld(this.worldName), x, y, z, yaw, pitch)

}