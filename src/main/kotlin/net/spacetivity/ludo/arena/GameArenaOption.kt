package net.spacetivity.ludo.arena

class GameArenaOption {

    enum class Status {
        CONFIGURATING,
        RESETTING,
        READY
    }

    enum class Phase {
        IDLE,
        INGAME,
        ENDING
    }

}