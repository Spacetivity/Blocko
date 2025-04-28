package net.spacetivity.blocko.phase.impl

import net.spacetivity.blocko.achievement.grantIfCompletedBy
import net.spacetivity.blocko.achievement.impl.BadLuckAchievement
import net.spacetivity.blocko.achievement.impl.PlayFirstGameAchievement
import net.spacetivity.blocko.countdown.impl.EndingCountdown
import net.spacetivity.blocko.phase.GamePhase
import org.bukkit.inventory.ItemStack

class EndingPhase(arenaId: String) : GamePhase(arenaId, "ending", 2, EndingCountdown(arenaId)) {

    override fun start() {
        val gameArena = getArena()

        for (gamePlayer in gameArena.currentPlayers) {
            gamePlayer.grantIfCompletedBy(PlayFirstGameAchievement::class)
            gamePlayer.grantIfCompletedBy(BadLuckAchievement::class)
        }

        countdown?.tryStartup()
    }

    override fun stop() {

    }

    override fun initPhaseHotbarItems(hotbarItems: MutableMap<Int, ItemStack>) {

    }

    override fun initSpectatorHotbarItems(hotbarItems: MutableMap<Int, ItemStack>) {

    }

}