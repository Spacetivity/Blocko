package net.spacetivity.ludo.field

import net.spacetivity.ludo.utils.PathFace

class TurnComponent(val facing: PathFace) {

    fun getRotation(): Float = this.facing.radians

}