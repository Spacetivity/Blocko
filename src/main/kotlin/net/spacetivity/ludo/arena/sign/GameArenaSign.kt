package net.spacetivity.ludo.arena.sign

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import org.bukkit.Location

data class GameArenaSign(val location: Location, var arenaId: String?) {

    fun getArena(): GameArena? {
        return if (this.arenaId == null) null else LudoGame.instance.gameArenaHandler.getArena(this.arenaId!!)
    }

}