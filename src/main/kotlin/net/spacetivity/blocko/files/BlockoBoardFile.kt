package net.spacetivity.blocko.files

import net.spacetivity.blocko.utils.FileUtils
import org.bukkit.Material
import java.nio.file.Path

//TODO: in the future, move this to arena specific storage
data class BlockoBoardFile(
    val gameFieldBlockType: String = Material.BONE_BLOCK.name,

    val redTeamSpawnBlockType: String = Material.RED_GLAZED_TERRACOTTA.name,
    val redGarageFieldBlockType: String = Material.RED_CONCRETE.name,

    val greenTeamSpawnBlockType: String = Material.GREEN_GLAZED_TERRACOTTA.name,
    val greenGarageFieldBlockType: String = Material.GREEN_CONCRETE.name,

    val blueTeamSpawnBlockType: String = Material.BLUE_GLAZED_TERRACOTTA.name,
    val blueGarageFieldBlockType: String = Material.BLUE_CONCRETE.name,

    val yellowTeamSpawnBlockType: String = Material.YELLOW_GLAZED_TERRACOTTA.name,
    val yellowGarageFieldBlockType: String = Material.YELLOW_CONCRETE.name,
) : SpaceFile {

    override fun createOrLoad(dataFolder: Path): SpaceFile {
        return FileUtils.createOrLoadFile(dataFolder, "global", "blocko_board_settings", BlockoBoardFile::class, this)
    }

}
