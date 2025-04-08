package net.spacetivity.blocko.field

import net.spacetivity.blocko.utils.Constants

enum class PathFace(val headValue: String, val radians: Float) {

    NORTH(Constants.NORTH, 180.0f),
    EAST(Constants.EAST, 270.0f),
    SOUTH(Constants.SOUTH, 0.0f),
    WEST(Constants.WEST, 90.0f)

}
