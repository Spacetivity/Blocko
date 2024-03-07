package net.spacetivity.ludo.entity

enum class GameEntityStatus(val display: String) {
    AT_SPAWN("At Spawn"),
    ON_FIELD("On Field"),
    MOVING("Moving"),
    SAVED("Saved");
}