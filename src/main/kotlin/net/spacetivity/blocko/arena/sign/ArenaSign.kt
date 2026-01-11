package net.spacetivity.blocko.arena.sign

import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.arena.Arena
import net.spacetivity.blocko.arena.id.ArenaId
import org.bukkit.Location

data class ArenaSign(val location: Location, var arenaId: ArenaId?) {

    fun getArena(): Arena? {
        return if (this.arenaId == null) null else Blocko.instance.arenaHandler.getArena(this.arenaId!!)
    }

}