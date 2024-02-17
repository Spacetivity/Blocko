package net.spacetivity.ludo.player

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.entity.GameEntity
import net.spacetivity.ludo.extensions.isDicing
import net.spacetivity.ludo.extensions.startDicing
import net.spacetivity.ludo.phase.GamePhaseMode
import net.spacetivity.ludo.phase.impl.IngamePhase
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

class GamePlayer(val uuid: UUID, val arenaId: String, val teamName: String, val isAI: Boolean, var dicedNumber: Int?) {

    var activeEntity: GameEntity? = null
    var inAction: Boolean = false

    var isPicking: Boolean = false
    var isMoving: Boolean = false

    fun dice() {
        if (isDicing()) return
        this.startDicing()
    }

    fun autoPickEntity(ingamePhase: IngamePhase) {
        if (this.isPicking) return
        if (this.dicedNumber == null) return

        if (!this.isPicking) this.isPicking = true

        val situation: Pair<AI_EntityPickRule, GameEntity?> = AI_EntityPickRule.analyzeCurrentRuleSituation(this, this.dicedNumber!!)
        if (situation.second != null) this.activeEntity = situation.second!!

        println("TRIED PICKING A ENTITY FOR TEAM $teamName with result ${situation.first.name}")

        // If there is no entity available for moving forward, the game moves on to the next team to play
        if (situation.first != AI_EntityPickRule.NOT_MOVABLE) {
            this.activeEntity = null
            ingamePhase.phaseMode = GamePhaseMode.DICE
            ingamePhase.setNextControllingTeam()
            this.isPicking = false
            return
        }

        this.isPicking = false
        ingamePhase.phaseMode = GamePhaseMode.MOVE_ENTITY
    }

    fun movePickedEntity() {
        if (isMoving) return
        if (this.dicedNumber == null) return
        if (this.activeEntity == null) return

        if (!this.inAction) this.isMoving = true

        val currentFieldId: Int? = this.activeEntity!!.currentFieldId

        val teamStartPoint = 0
        this.activeEntity?.newGoalFieldId = if (currentFieldId == null) teamStartPoint + this.dicedNumber!! else currentFieldId + this.dicedNumber!!

        if (!this.activeEntity!!.shouldMove) this.activeEntity!!.shouldMove = true
        if (this.activeEntity!!.controller == null) this.activeEntity!!.controller = this
    }

    fun hasWon(): Boolean {
        return LudoGame.instance.gameEntityHandler.getEntitiesFromTeam(this.arenaId, this.teamName).all { it.isInGarage() && !it.isMovable(1) }
    }

    fun toBukkitInstance(): Player? {
        return Bukkit.getPlayer(this.uuid)
    }

}