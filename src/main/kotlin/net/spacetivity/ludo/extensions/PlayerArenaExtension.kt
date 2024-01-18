package net.spacetivity.ludo.extensions

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import org.bukkit.entity.Player

fun Player.isPlaying(): Boolean {
    return getArena() != null
}

fun Player.getArena(): GameArena? {
    return LudoGame.instance.gameArenaHandler.getArenaOfPlayer(uniqueId)
}
