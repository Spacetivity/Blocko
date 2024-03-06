package net.spacetivity.ludo.phase.impl

import net.kyori.adventure.text.Component
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.achievement.impl.PlayFirstGameAchievement
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.countdown.impl.EndingCountdown
import net.spacetivity.ludo.phase.GamePhase
import net.spacetivity.ludo.player.GamePlayer
import org.bukkit.inventory.ItemStack

class EndingPhase(arenaId: String) : GamePhase(arenaId, "ending", 2, EndingCountdown(arenaId)) {

    override fun start() {
        println("Phase $name started in arena $arenaId!")

        val gameArena: GameArena = getArena()
        gameArena.sendArenaMessage(Component.text("The game ends now..."))

        for (gamePlayer: GamePlayer in gameArena.currentPlayers) {
            LudoGame.instance.achievementHandler.getAchievement(PlayFirstGameAchievement::class.java)?.grantIfCompletedBy(gamePlayer)
        }

        countdown?.tryStartup()
    }

    override fun stop() {
        println("Phase $name stopped in arena $arenaId!")
    }

    override fun initPhaseHotbarItems(hotbarItems: MutableMap<Int, ItemStack>) {

    }

}