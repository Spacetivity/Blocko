package net.spacetivity.blocko.extensions

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.player.GamePlayer
import org.bukkit.entity.Player

fun Player.getArena(): GameArena? {
    return BlockoGame.instance.gameArenaHandler.getArenaOfPlayer(uniqueId)
}

fun Player.isSpectating(): Boolean {
    return BlockoGame.instance.gameArenaHandler.cachedArenas.any { it.spectatorPlayers.contains(this.uniqueId) }
}

fun Player.clearPhaseItems() {
    val gameArena: GameArena = getArena() ?: return
    gameArena.phase.clearPlayerInventory(this)
}

fun Player.getPossibleInvitationDestination(): GameArena? {
    return BlockoGame.instance.gameArenaHandler.cachedArenas.firstOrNull { it.invitedPlayers.contains(this.uniqueId) }
}

fun Player.toGamePlayerInstance(): GamePlayer? {
    return getArena()?.currentPlayers?.find { it.uuid == this.uniqueId }
}