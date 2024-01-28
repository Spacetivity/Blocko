package net.spacetivity.ludo.phase.impl

import net.kyori.adventure.text.Component
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.phase.GamePhase
import net.spacetivity.ludo.phase.GamePhaseMode
import net.spacetivity.ludo.team.GameTeam
import net.spacetivity.ludo.utils.ItemUtils
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.*

class IngamePhase(arenaId: String) : GamePhase(arenaId, "ingame", 1, null) {

    private var controllingTeamId: Int = 0
    var phaseMode: GamePhaseMode = GamePhaseMode.DICING

    override fun start() {
        println("Phase $name started in arena $arenaId!")
        getArenaPlayers().forEach { setupPlayerInventory(it) }
    }

    override fun stop() {
        println("Phase $name stopped in arena $arenaId!")
    }

    override fun initPhaseHotbarItems(hotbarItems: MutableMap<Int, ItemStack>) {
        hotbarItems[0] = LudoGame.instance.diceHandler.getDiceItem()
        for ((entityIndex, i) in (2 .. 5).withIndex()) {
            hotbarItems[i] = ItemUtils(Material.ARMOR_STAND)
                .setName(Component.text("Move Entity #$entityIndex"))
                .build()
        }
    }

    fun isInControllingTeam(uuid: UUID): Boolean {
        return getControllingTeam()?.teamMembers?.contains(uuid) ?: false
    }

    fun getControllingTeam(): GameTeam? {
        return LudoGame.instance.gameTeamHandler.gameTeams.get(this.arenaId).find { it.teamId == this.controllingTeamId }
    }

}