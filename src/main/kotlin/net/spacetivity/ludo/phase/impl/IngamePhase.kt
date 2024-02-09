package net.spacetivity.ludo.phase.impl

import net.kyori.adventure.text.Component
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.phase.GamePhase
import net.spacetivity.ludo.phase.GamePhaseMode
import net.spacetivity.ludo.team.GameTeam
import net.spacetivity.ludo.utils.ItemBuilder
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.*

class IngamePhase(arenaId: String) : GamePhase(arenaId, "ingame", 1, null) {

    private var controllingTeamId: Int = 0
    var phaseMode: GamePhaseMode = GamePhaseMode.DICE

    override fun start() {
        println("Phase $name started in arena $arenaId!")
        getArena().currentPlayers.filter { !it.isAI }.map { it.toBukkitInstance()!! }.forEach { setupPlayerInventory(it) }
    }

    override fun stop() {
        println("Phase $name stopped in arena $arenaId!")
    }

    override fun initPhaseHotbarItems(hotbarItems: MutableMap<Int, ItemStack>) {
        hotbarItems[0] = LudoGame.instance.diceHandler.getDiceItem()
        for ((entityIndex, i) in (2..5).withIndex()) {
            hotbarItems[i] = ItemBuilder(Material.ARMOR_STAND)
                .setName(Component.text("Move Entity #$entityIndex"))
                .build()
        }
    }

    fun isInControllingTeam(uuid: UUID): Boolean {
        return getControllingTeam()?.teamMembers?.contains(uuid) ?: false
    }

    fun setNextControllingTeam(): GameTeam? {
        this.controllingTeamId = 0
        return getControllingTeam()

//        val newControllingTeam: GameTeam? = LudoGame.instance.gameTeamHandler.gameTeams.get(this.arenaId).find { it.teamId == this.controllingTeamId.inc() }
//        val newControllingTeamId: Int = newControllingTeam?.teamId ?: 0
//        this.controllingTeamId = newControllingTeamId
//        return getControllingTeam()
    }

    fun getControllingTeam(): GameTeam? {
        return LudoGame.instance.gameTeamHandler.gameTeams.get(this.arenaId).find { it.teamId == this.controllingTeamId }
    }

}