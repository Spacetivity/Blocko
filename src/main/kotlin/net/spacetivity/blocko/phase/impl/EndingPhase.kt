package net.spacetivity.blocko.phase.impl

import net.kyori.adventure.text.Component
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.achievement.impl.PlayFirstGameAchievement
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.countdown.impl.EndingCountdown
import net.spacetivity.blocko.phase.GamePhase
import net.spacetivity.blocko.player.GamePlayer
import org.bukkit.inventory.ItemStack

class EndingPhase(arenaId: String) : GamePhase(arenaId, "ending", 2, EndingCountdown(arenaId)) {

    override fun start() {
        val gameArena: GameArena = getArena()
        gameArena.sendArenaMessage(Component.text("The game ends now..."))

        for (gamePlayer: GamePlayer in gameArena.currentPlayers) {
            BlockoGame.instance.achievementHandler.getAchievement(PlayFirstGameAchievement::class.java)?.grantIfCompletedBy(gamePlayer)
        }

        countdown?.tryStartup()
    }

    override fun stop() {

    }

    override fun initPhaseHotbarItems(hotbarItems: MutableMap<Int, ItemStack>) {

    }

}