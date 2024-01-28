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