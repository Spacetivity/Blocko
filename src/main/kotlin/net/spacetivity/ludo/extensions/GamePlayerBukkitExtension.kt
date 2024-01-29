package net.spacetivity.ludo.extensions

import net.kyori.adventure.text.Component
import net.spacetivity.ludo.player.GamePlayer
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

fun GamePlayer.sendMessage(component: Component) {
    if (isAI) return
    val player: Player = toBukkitInstance() ?: return
    player.sendMessage(component)
}

fun GamePlayer.sendActionBar(component: Component) {
    if (isAI) return
    val player: Player = toBukkitInstance() ?: return
    player.sendActionBar(component)
}

fun GamePlayer.playSound(sound: Sound) {
    if (isAI) return
    val player: Player = toBukkitInstance() ?: return
    player.playSound(player.location, sound, 0.2F, 1.0F)
}

fun GamePlayer.clearSlot(slot: Int) {
    if (isAI) return
    val player: Player = toBukkitInstance() ?: return
    player.inventory.clear(slot)
}

fun GamePlayer.accessStorageContents(): Array<ItemStack?>? {
    if (isAI) return null
    val player: Player = toBukkitInstance() ?: return null
    return player.inventory.storageContents
}