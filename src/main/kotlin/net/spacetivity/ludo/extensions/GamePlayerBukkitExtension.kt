package net.spacetivity.ludo.extensions

import net.kyori.adventure.text.Component
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.team.GameTeam
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

fun GamePlayer.getTeam(): GameTeam {
    return LudoGame.instance.gameTeamHandler.getTeamOfPlayer(this.arenaId, this.uuid)!!
}

fun GamePlayer.sendMessage(component: Component) {
    if (!isAI) {
        val player: Player = toBukkitInstance() ?: return
        player.sendMessage(component)
    }
}

fun GamePlayer.sendActionBar(component: Component) {
    if (!isAI) {
        val player: Player = toBukkitInstance() ?: return
        player.sendActionBar(component)
    }
}

fun GamePlayer.playSound(sound: Sound) {
    if (!isAI) {
        val player: Player = toBukkitInstance() ?: return
        player.playSound(player.location, sound, 0.2F, 1.0F)
    }
}

fun GamePlayer.setGameMode(gameMode: GameMode) {
    if (!isAI) {
        val player: Player = toBukkitInstance() ?: return
        player.gameMode = gameMode
    }
}

fun GamePlayer.setFlying(shouldFly: Boolean) {
    if (!isAI) {
        val player: Player = toBukkitInstance() ?: return
        player.allowFlight = shouldFly
        player.isFlying = shouldFly
    }
}

fun GamePlayer.accessStorageContents(): Array<ItemStack?>? {
    if (!isAI) {
        val player: Player = toBukkitInstance() ?: return null
        return player.inventory.storageContents
    }

    return null
}