package net.spacetivity.ludo.extensions

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.dice.DiceSession
import net.spacetivity.ludo.player.GamePlayer

fun GamePlayer.isDicing(): Boolean {
    return getDiceSession() != null
}

fun GamePlayer.getDiceSession(): DiceSession? {
    return LudoGame.instance.diceHandler.dicingPlayers[uuid]
}