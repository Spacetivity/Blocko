package net.spacetivity.ludo.achievement.container

interface ProgressRequirement<T : Number> : Requirement {

    val neededCount: T

    fun getProgress(currentCount: T): Double = (currentCount.toDouble() / neededCount.toDouble()) * 100

}