package net.spacetivity.blocko.files

import net.spacetivity.blocko.utils.FileUtils
import org.bukkit.Material
import java.nio.file.Path

data class GlobalConfigFile(
    val language: String = "en_US",
    val setupItemType: String = Material.GOLDEN_HOE.name,

    val gameArenaAutoJoin: Boolean = false,
    val gameArenaMaxParallelAmount: Int = 10,

    val coinsPerElimination: Int = 20,

    val idleCountdownSeconds: Int = 30,
    val endingCountdownSeconds: Int = 10,

    val motdEnabled: Boolean = true
) : SpaceFile {

    override fun createOrLoad(dataFolder: Path): SpaceFile {
        return FileUtils.createOrLoadFile(dataFolder, "global", "config", GlobalConfigFile::class, this)
    }

}
