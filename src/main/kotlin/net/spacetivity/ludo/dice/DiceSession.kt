package net.spacetivity.ludo.dice

class DiceSession(var currentDiceNumber: Int, var dicingEndTimestamp: Long) {

    fun gainedNewDice(): Boolean {
        return this.currentDiceNumber == 6
    }

}