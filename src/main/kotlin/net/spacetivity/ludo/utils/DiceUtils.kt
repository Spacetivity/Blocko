package net.spacetivity.ludo.utils

import kotlin.random.Random

object DiceUtils {

    fun rollTheDice(): Int {
        return Random.nextInt(1, 6)
    }

}