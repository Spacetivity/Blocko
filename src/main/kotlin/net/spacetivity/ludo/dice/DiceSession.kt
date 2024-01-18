package net.spacetivity.ludo.dice

class DiceSession(var currentDiceNumber: Int) {

    fun gainedNewDice(): Boolean {
        return this.currentDiceNumber == 6
    }

}