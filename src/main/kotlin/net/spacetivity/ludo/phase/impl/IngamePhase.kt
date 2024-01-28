package net.spacetivity.ludo.phase.impl

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.phase.GamePhase
import net.spacetivity.ludo.phase.GamePhaseMode
import net.spacetivity.ludo.team.GameTeam
import org.bukkit.inventory.ItemStack
import java.util.*

class IngamePhase(arenaId: String) : GamePhase(arenaId, "ingame", 1, null) {

    private var controllingTeamId: Int = 0
    var phaseMode: GamePhaseMode = GamePhaseMode.DICING

    override fun start() {
        println("Phase $name started in arena $arenaId!")

        getArena().reset()
    }

    override fun stop() {
        println("Phase $name stopped in arena $arenaId!")
    }

    override fun initPhaseHotbarItems(hotbarItems: MutableMap<Int, ItemStack>) {

    }

    fun isInControllingTeam(uuid: UUID): Boolean {
        return getControllingTeam()?.teamMembers?.contains(uuid) ?: false
    }

    fun getControllingTeam(): GameTeam? {
        return LudoGame.instance.gameTeamHandler.gameTeams.get(this.arenaId).find { it.teamId == this.controllingTeamId }
    }

}