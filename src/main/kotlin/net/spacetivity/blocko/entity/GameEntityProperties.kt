package net.spacetivity.blocko.entity

data class GameEntityProperties(val price: Int, val isBaby: Boolean, val achievementKey: String = "-/-") {

    fun requiresAchievement(): Boolean = this.achievementKey != "-/-"

}