package net.spacetivity.ludo.extensions

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.player.GamePlayer
import org.bukkit.entity.Player

fun Player.getArena(): GameArena? {
    return LudoGame.instance.gameArenaHandler.getArenaOfPlayer(uniqueId)
}

fun Player.clearPhaseItems() {
    val gameArena: GameArena = getArena() ?: return
    gameArena.phase.clearPlayerInventory(this)
}

fun Player.getPossibleInvitationDestination(): GameArena? {
    return LudoGame.instance.gameArenaHandler.cachedArenas.firstOrNull { it.invitedPlayers.contains(this.uniqueId) }
}

fun Player.toGamePlayerInstance(): GamePlayer? {
    return getArena()?.currentPlayers?.find { it.uuid == this.uniqueId }
}