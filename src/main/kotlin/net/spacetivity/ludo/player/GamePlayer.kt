package net.spacetivity.ludo.player

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.entity.GameEntity
import net.spacetivity.ludo.extensions.startDicing
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

class GamePlayer(val uuid: UUID, val arenaId: String, val teamName: String, val isAI: Boolean, var dicedNumber: Int?) {

    var activeEntity: GameEntity? = null
    var inAction: Boolean = false

    fun dice() {
        if (inAction) return
        this.startDicing()
    }

    //TODO: Implement ai intelligence. Not only pic a entity if its movable, pic it also when its movable AND in the goal field is a enemy to throw out (EXPERIMENTAL)
    // this is only valid for ai players
    fun autoPickEntity() {
        if (inAction) return
        if (this.dicedNumber == null) return
        inAction = true

        val situation: Pair<AI_EntityPickRule, GameEntity?> = AI_EntityPickRule.analyzeCurrentRuleSituation(this, this.dicedNumber!!)
        if (situation.second != null) this.activeEntity = situation.second!!

        println("TRIED PICKING A ENTITY FOR TEAM $teamName with result ${situation.first.name}")

        inAction = false
    }

    fun movePickedEntity() {
        if (inAction) return
        if (this.dicedNumber == null) return
        if (this.activeEntity == null) return

        if (!this.inAction) this.inAction = true

        val currentFieldId: Int? = this.activeEntity!!.currentFieldId

        val teamStartPoint = 0
        this.activeEntity?.newGoalFieldId = if (currentFieldId == null) teamStartPoint + this.dicedNumber!! else currentFieldId + this.dicedNumber!!


        if (!this.activeEntity!!.shouldMove) this.activeEntity!!.shouldMove = true
        if (this.activeEntity!!.controller == null) this.activeEntity!!.controller = this
//        if (MetadataUtils.has(this.activeEntity.livingEntity, "inGarage"))
    }

    fun hasWon(): Boolean {
        return LudoGame.instance.gameEntityHandler.getEntitiesFromTeam(this.arenaId, this.teamName).all { !it.isMovable(1) }
    }

    fun toBukkitInstance(): Player? {
        return Bukkit.getPlayer(this.uuid)
    }

}