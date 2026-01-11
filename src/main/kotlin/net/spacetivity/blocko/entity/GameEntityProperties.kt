package net.spacetivity.blocko.entity

import net.spacetivity.blocko.utils.Constants

data class GameEntityProperties(val price: Int, val isBaby: Boolean, val achievementKey: String = Constants.PLACEHOLDER) {

    fun requiresAchievement(): Boolean = this.achievementKey != Constants.PLACEHOLDER

}