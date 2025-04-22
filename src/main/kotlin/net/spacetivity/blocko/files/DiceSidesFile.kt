package net.spacetivity.blocko.files

import net.spacetivity.blocko.utils.Constants
import net.spacetivity.blocko.utils.FileUtils
import java.nio.file.Path

data class DiceSidesFile(
    val diceSides: MutableMap<Int, String> = mutableMapOf(
        Pair(1, Constants.DICE_ONE_SKULL),
        Pair(2, Constants.DICE_TWO_SKULL),
        Pair(3, Constants.DICE_THREE_SKULL),
        Pair(4, Constants.DICE_FOUR_SKULL),
        Pair(5, Constants.DICE_FIVE_SKULL),
        Pair(6, Constants.DICE_SIX_SKULL)
    ),
) : SpaceFile {

    override fun createOrLoad(dataFolder: Path): SpaceFile {
        return FileUtils.createOrLoadFile(dataFolder, "dice", "dice_sides", DiceSidesFile::class, this)
    }

}