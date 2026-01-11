package net.spacetivity.blocko.files

import java.nio.file.Path

interface SpaceFile {

    fun createOrLoad(dataFolder: Path): SpaceFile

}