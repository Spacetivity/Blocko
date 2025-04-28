package net.spacetivity.blocko.player

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.team.GameTeam
import org.bukkit.Sound
import org.bukkit.inventory.ItemStack

fun GamePlayer.getTeam(): GameTeam {
    return BlockoGame.instance.gameTeamHandler.getTeamOfPlayer(this.arenaId, this.uuid)!!
}

fun GamePlayer.playSound(sound: Sound) {
    if (isAI) return
    val player = toBukkitInstance() ?: return
    player.playSound(player.location, sound, 0.2F, 1.0F)
}

fun GamePlayer.accessStorageContents(): Array<ItemStack?>? {
    if (isAI) return null
    return toBukkitInstance()?.inventory?.storageContents
}