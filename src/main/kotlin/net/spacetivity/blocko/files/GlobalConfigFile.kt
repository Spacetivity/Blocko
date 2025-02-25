package net.spacetivity.blocko.files

data class GlobalConfigFile(
    val language: String,
    val setupItemType: String,

    val gameArenaAutoJoin: Boolean,
    val gameArenaMaxParallelAmount: Int,

    val coinsPerElimination: Int,

    val idleCountdownSeconds: Int,
    val endingCountdownSeconds: Int,

    val motdEnabled: Boolean
) : SpaceFile

enum class DatabaseType {
    SQLITE,
    MARIADB;
}
