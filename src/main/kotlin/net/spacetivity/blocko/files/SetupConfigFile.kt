package net.spacetivity.blocko.files

data class SetupConfigFile(
    val setupStepsResettable: Boolean,
    val setupSessionEndless: Boolean,
    val setupSessionTimeoutMinutes: Int,
) : SpaceFile
