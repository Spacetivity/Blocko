package net.spacetivity.ludo.field

import net.spacetivity.ludo.utils.PathFace
import org.bukkit.entity.LivingEntity
import org.bukkit.util.Vector
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class TurnComponent(val facing: PathFace) {

    fun getRotation(entity: LivingEntity): Vector {
        val radians = this.facing.radians
        val x = cos(radians)
        val z = sin(radians)

        val directionVector = Vector(x, 0.0, z)

        val location = entity.location
        val pitch = location.pitch
        val yaw = atan2(-directionVector.x, directionVector.z)

        val directionX = cos(pitch.toDouble()) * cos(yaw)
        val directionY = sin(pitch.toDouble())
        val directionZ = cos(pitch.toDouble()) * sin(yaw)

        return Vector(directionX, directionY, directionZ)
    }

}