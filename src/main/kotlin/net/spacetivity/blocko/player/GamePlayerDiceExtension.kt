package net.spacetivity.blocko.player

import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.dice.DiceSession

fun GamePlayer.isDicing(): Boolean {
    return getDiceSession() != null
}

fun GamePlayer.getDiceSession(): DiceSession? {
    return Blocko.instance.diceHandler.dicingPlayers[uuid]
}