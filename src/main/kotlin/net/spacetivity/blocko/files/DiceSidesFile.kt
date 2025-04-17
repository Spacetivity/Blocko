package net.spacetivity.blocko.files

import net.spacetivity.blocko.utils.Constants
import net.spacetivity.blocko.utils.FileUtils
import java.nio.file.Path

data class DiceSidesFile(
    val diceSides: MutableMap<Int, String> = mutableMapOf(
        Pair(1, Constants.DICE_ONE),
        Pair(2, Constants.DICE_TWO),
        Pair(3, Constants.DICE_THREE),
        Pair(4, Constants.DICE_FOUR),
        Pair(5, Constants.DICE_FIVE),
        Pair(6, Constants.DICE_SIX)
    ),
) : SpaceFile {

    override fun createOrLoad(dataFolder: Path): SpaceFile {
        return FileUtils.createOrLoadFile(dataFolder, "dice", "dice_sides", DiceSidesFile::class, this)
    }

}