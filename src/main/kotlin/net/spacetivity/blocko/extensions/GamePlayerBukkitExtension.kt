package net.spacetivity.blocko.extensions

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.team.GameTeam
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

fun GamePlayer.getTeam(): GameTeam {
    return BlockoGame.instance.gameTeamHandler.getTeamOfPlayer(this.arenaId, this.uuid)!!
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