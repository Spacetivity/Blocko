package net.spacetivity.blocko.player

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.dice.DiceSession

fun GamePlayer.isDicing(): Boolean {
    return getDiceSession() != null
}

fun GamePlayer.getDiceSession(): DiceSession? {
    return BlockoGame.instance.diceHandler.dicingPlayers[uuid]
}