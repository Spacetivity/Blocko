package net.spacetivity.ludo.extensions

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.dice.DiceSession
import net.spacetivity.ludo.player.GamePlayer

fun GamePlayer.isDicing(): Boolean {
    return getDiceSession() != null
}

fun GamePlayer.startDicing() {
    LudoGame.instance.diceHandler.startDicing(this)
}

fun GamePlayer.stopDicing() {
    LudoGame.instance.diceHandler.stopDicing(this)
}

fun GamePlayer.readDicedNumber(): Int? {
    if (this.dicedNumber == null) return null
    val dn = this.dicedNumber
    this.dicedNumber = null
    return dn
}

fun GamePlayer.getCurrentDiceNumber(): Int? {
    return getDiceSession()?.currentDiceNumber
}

fun GamePlayer.setCurrentDiceNumber(currentDiceNumber: Int) {
    getDiceSession()?.currentDiceNumber = currentDiceNumber
}

fun GamePlayer.getDiceSession(): DiceSession? {
    return LudoGame.instance.diceHandler.dicingPlayers[uuid]
}