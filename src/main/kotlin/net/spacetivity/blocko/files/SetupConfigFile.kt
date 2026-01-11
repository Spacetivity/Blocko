package net.spacetivity.blocko.files

import net.spacetivity.blocko.utils.FileUtils
import java.nio.file.Path

data class SetupConfigFile(
    val setupStepsResettable: Boolean = true,
    val setupSessionEndless: Boolean = false,
    val setupSessionTimeoutMinutes: Int = 15, //TODO: make timeout only start if player in setup is AFK
) : SpaceFile {

    override fun createOrLoad(dataFolder: Path): SpaceFile {
        return FileUtils.createOrLoadFile(dataFolder, "global", "setup", SetupConfigFile::class, this)
    }

}
