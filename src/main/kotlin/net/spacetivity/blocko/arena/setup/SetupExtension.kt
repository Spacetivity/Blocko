package net.spacetivity.blocko.arena.setup

import net.spacetivity.blocko.BlockoGame
import org.bukkit.entity.Player

fun Player.getSetupSession(): ArenaSetupSession? {
    return BlockoGame.instance.arenaSetupHandler.getSetupData(this.uniqueId)
}