package net.spacetivity.blocko.files

data class GlobalConfigFile(
    val language: String,
    val setupItemType: String,

    val gameLobbyWorld: String,
    val gameLobbyProtected: Boolean,

    val gameArenaAutoJoin: Boolean,
    val gameArenaMaxParallelAmount: Int,

    val coinsPerElimination: Int
) : SpaceFile
