package net.spacetivity.ludo.extensions

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.dice.DiceSession
import net.spacetivity.ludo.utils.MetadataUtils
import org.bukkit.entity.Player

fun Player.isDicing(): Boolean {
    return getDiceSession() != null
}

fun Player.startDicing() {
    LudoGame.instance.diceHandler.startDicing(this)
}

fun Player.stopDicing() {
    LudoGame.instance.diceHandler.stopDicing(this)
}

/**
 * Reads the diced number associated with the player using metadata.
 *
 * This function retrieves the diced number stored in the player's metadata,
 * removes it from the metadata, and returns the value.
 *
 * @return The diced number if available, or `null` if not present in the metadata.
 */
fun Player.readDicedNumber(): Int? {
    val dicedNumber: Int = MetadataUtils.get(this, "dicedNumber")?: return null
    MetadataUtils.remove(this, "dicedNumber")
    return dicedNumber
}

fun Player.getCurrentDiceNumber(): Int? {
    return getDiceSession()?.currentDiceNumber
}

fun Player.setCurrentDiceNumber(currentDiceNumber: Int) {
    getDiceSession()?.currentDiceNumber = currentDiceNumber
}

fun Player.getDiceSession(): DiceSession? {
    return LudoGame.instance.diceHandler.dicingPlayers[uniqueId]
}