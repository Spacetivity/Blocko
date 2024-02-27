package net.spacetivity.ludo.utils

import org.bukkit.Location

object LocationUtils {

    fun centerLocation(rawLocation: Location): Location {
        val tempY = rawLocation.y
        val location = Location(rawLocation.world, rawLocation.x, rawLocation.y, rawLocation.z, rawLocation.yaw, rawLocation.pitch)
        val fixedLocation: Location = location.clone().toCenterLocation()
        fixedLocation.y = tempY
        return fixedLocation
    }

}