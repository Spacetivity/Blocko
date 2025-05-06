package net.spacetivity.blocko.arena

import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.player.GamePlayer
import org.bukkit.entity.Player

fun Player.getArena(): Arena? {
    return Blocko.instance.arenaHandler.getArenaOfPlayer(uniqueId)
}

fun Player.isSpectating(): Boolean {
    return Blocko.instance.arenaHandler.cachedArenas.any { it.spectatorPlayers.contains(this.uniqueId) }
}

fun Player.clearPhaseItems() {
    val gameArena = getArena() ?: return
    gameArena.phase.clearPlayerInventory(this)
}

fun Player.getPossibleInvitationDestination(): Arena? {
    return Blocko.instance.arenaHandler.cachedArenas.firstOrNull { it.invitedPlayers.contains(this.uniqueId) }
}

fun Player.toGamePlayerInstance(): GamePlayer? {
    return getArena()?.currentPlayers?.find { it.uuid == this.uniqueId }
}