package net.spacetivity.ludo

import net.spacetivity.ludo.entity.GameEntityHandler
import net.spacetivity.ludo.team.GameTeamHandler
import org.bukkit.plugin.java.JavaPlugin

class LudoGame : JavaPlugin() {

    lateinit var gameTeamHandler: GameTeamHandler
    lateinit var gameEntityHandler: GameEntityHandler

    override fun onEnable() {
        instance = this
        this.gameTeamHandler = GameTeamHandler()
        this.gameEntityHandler = GameEntityHandler()
    }

    override fun onDisable() {

    }

    companion object {
        @JvmStatic
        lateinit var instance: LudoGame
            private set
    }

}