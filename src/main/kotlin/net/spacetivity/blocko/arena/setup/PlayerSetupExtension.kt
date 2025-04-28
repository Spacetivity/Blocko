package net.spacetivity.blocko.arena.setup

import net.spacetivity.blocko.BlockoGame
import org.bukkit.entity.Player

fun Player.getSetupSession(): GameArenaSetupSession? {
    return BlockoGame.instance.gameArenaSetupHandler.getSetupData(this.uniqueId)
}