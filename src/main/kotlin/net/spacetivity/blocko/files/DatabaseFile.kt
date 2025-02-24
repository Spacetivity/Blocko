package net.spacetivity.blocko.files

data class DatabaseFile(
    val databaseType: DatabaseType,
    val hostname: String,
    val port: Int,
    val database: String,
    val user: String,
    val password: String,
) : SpaceFile