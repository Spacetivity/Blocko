package net.spacetivity.blocko.achievement.container

interface ProgressRequirement<T : Number> : Requirement {

    val neededCount: T

    fun getProgress(currentCount: T): Double = (currentCount.toDouble() / neededCount.toDouble()) * 100

}