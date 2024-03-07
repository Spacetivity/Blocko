package net.spacetivity.ludo.field

import net.spacetivity.ludo.utils.HeadUtils

enum class PathFace(val headValue: String, val radians: Float) {

    NORTH(HeadUtils.NORTH, 180.0f),
    EAST(HeadUtils.EAST, 270.0f),
    SOUTH(HeadUtils.SOUTH, 0.0f),
    WEST(HeadUtils.WEST, 90.0f)

}
