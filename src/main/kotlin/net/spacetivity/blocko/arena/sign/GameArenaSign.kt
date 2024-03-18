package net.spacetivity.blocko.arena.sign

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.GameArena
import org.bukkit.Location

data class GameArenaSign(val location: Location, var arenaId: String?) {

    fun getArena(): GameArena? {
        return if (this.arenaId == null) null else BlockoGame.instance.gameArenaHandler.getArena(this.arenaId!!)
    }

}