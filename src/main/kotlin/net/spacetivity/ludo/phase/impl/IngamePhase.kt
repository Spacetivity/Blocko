package net.spacetivity.ludo.phase.impl

import net.kyori.adventure.text.Component
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.phase.GamePhase
import net.spacetivity.ludo.phase.GamePhaseMode
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.team.GameTeam
import net.spacetivity.ludo.utils.ItemBuilder
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.*

class IngamePhase(arenaId: String) : GamePhase(arenaId, "ingame", 1, null) {

    var lastControllingTeamId: Int? = null
    var controllingTeamId: Int? = null

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
                .setData("entitySelector", entityIndex)
                .build()
        }
    }

    fun isInControllingTeam(uuid: UUID): Boolean {
        return getControllingTeam()?.teamMembers?.contains(uuid) ?: false
    }

    fun setNextControllingTeam(): GameTeam? {
        val availableTeams: List<GameTeam> = LudoGame.instance.gameTeamHandler.gameTeams[this.arenaId].filter { it.teamMembers.size == 1 && !it.deactivated }
        val newControllingTeam: GameTeam? = if (hasControllingTeamMemberDicedSix()) this.getControllingTeam() else availableTeams.find { it.teamId > this.controllingTeamId!! }

        // If no available team with a higher teamId is found, the team with the smallest teamId is used
        val newControllingTeamId: Int = newControllingTeam?.teamId ?: availableTeams.minOf { it.teamId }

        this.lastControllingTeamId = if (this.controllingTeamId == null) null else this.controllingTeamId //TODO: test that

        this.controllingTeamId = newControllingTeamId

        return getControllingTeam()
    }

    fun getControllingTeam(): GameTeam? {
        return LudoGame.instance.gameTeamHandler.gameTeams.get(this.arenaId).find { it.teamId == this.controllingTeamId }
    }

    private fun hasControllingTeamMemberDicedSix(): Boolean {
        val controllingTeam: GameTeam = getControllingTeam() ?: return false
        if (controllingTeam.deactivated) return false
        val gameArena: GameArena = LudoGame.instance.gameArenaHandler.getArena(this.arenaId) ?: return false

        var hasDicedSix = false

        for (teamMemberUniqueId: UUID in controllingTeam.teamMembers) {
            val gamePlayer: GamePlayer = gameArena.currentPlayers.find { it.uuid == teamMemberUniqueId } ?: continue
            if (gamePlayer.dicedNumber == null || gamePlayer.dicedNumber != 6) continue
            hasDicedSix = true
        }

        return hasDicedSix
    }

}