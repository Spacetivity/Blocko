package net.spacetivity.blocko.files

import net.spacetivity.blocko.utils.FileUtils
import java.nio.file.Path

data class DatabaseFile(
    val databaseType: DatabaseType = DatabaseType.SQLITE,
    val hostname: String = "-",
    val port: Int = 3306,
    val database: String = "-",
    val user: String = "-",
    val password: String = "-",
) : SpaceFile {

    override fun createOrLoad(dataFolder: Path): SpaceFile {
        return FileUtils.createOrLoadFile(dataFolder, "global", "mysql", DatabaseFile::class, this)
    }

}

enum class DatabaseType {
    SQLITE,
    MARIADB;
}