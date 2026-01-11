package net.spacetivity.blocko.files

import net.spacetivity.blocko.entity.GameEntityProperties
import net.spacetivity.blocko.utils.FileUtils
import java.nio.file.Path

data class GameEntityPropertiesFile(val gameEntityId: String, val gameEntityProperties: GameEntityProperties) : SpaceFile {

    override fun createOrLoad(dataFolder: Path): SpaceFile {
        return FileUtils.createOrLoadFile(dataFolder, "entities", "entity_${this.gameEntityId}_properties", GameEntityPropertiesFile::class, this)
    }

}
