package net.spacetivity.blocko.extensions

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.dice.DiceSession
import net.spacetivity.blocko.player.GamePlayer

fun GamePlayer.isDicing(): Boolean {
    return getDiceSession() != null
}

fun GamePlayer.getDiceSession(): DiceSession? {
    return BlockoGame.instance.diceHandler.dicingPlayers[uuid]
}