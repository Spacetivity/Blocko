package net.spacetivity.blocko.field

import net.spacetivity.blocko.utils.Constants

enum class GameFieldRotation(val headValue: String, val radians: Float) {

    NORTH(Constants.NORTH_SKULL, 180.0f),
    SOUTH(Constants.SOUTH_SKULL, 0.0f),
    EAST(Constants.EAST_SKULL, 270.0f),
    WEST(Constants.WEST_SKULL, 90.0f)

}
