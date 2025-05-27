package net.spacetivity.blocko.setup

import net.spacetivity.blocko.Blocko
import org.bukkit.entity.Player

fun Player.getSetupSession(): ArenaSetupSession? {
    return Blocko.instance.arenaSetupHandler.getSetupData(this.uniqueId)
}