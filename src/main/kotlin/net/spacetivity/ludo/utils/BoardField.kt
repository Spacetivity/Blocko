package net.spacetivity.ludo.utils

import net.spacetivity.ludo.entity.GameEntity
import org.bukkit.Location
import org.bukkit.World

interface BoardField {

    val id: Int
    val arenaId: String
    val world: World
    val x: Double
    val z: Double
    var isTaken: Boolean

    fun getWorldPosition(fieldHeight: Double): Location

    fun getCurrentHolder(): GameEntity?

}