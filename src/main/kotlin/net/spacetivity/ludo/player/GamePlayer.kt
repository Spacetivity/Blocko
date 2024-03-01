package net.spacetivity.ludo.player

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.entity.GameEntity
import net.spacetivity.ludo.extensions.isDicing
import net.spacetivity.ludo.phase.GamePhaseMode
import net.spacetivity.ludo.phase.impl.IngamePhase
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

class GamePlayer(val uuid: UUID, val arenaId: String, val teamName: String, val isAI: Boolean) {

    var dicedNumber: Int? = null
    var activeEntity: GameEntity? = null
    var lastEntityPickRule: EntityPickRule? = null

    fun dice(ingamePhase: IngamePhase) {
        if (isDicing()) return
        LudoGame.instance.diceHandler.startDicing(this, ingamePhase)
    }

    fun manuallyPickEntity(ingamePhase: IngamePhase, gameEntity: GameEntity) {
        if (this.dicedNumber == null) return
        this.activeEntity = gameEntity

        ingamePhase.phaseMode = GamePhaseMode.MOVE_ENTITY
    }

    fun autoPickEntity(ingamePhase: IngamePhase) {
        if (this.dicedNumber == null) return

        val situation: Pair<EntityPickRule, GameEntity?> = EntityPickRule.analyzeCurrentRuleSituation(this, this.dicedNumber!!)

        println("TRIED PICKING A ENTITY FOR TEAM $teamName with result ${situation.first.name}")

        // If there is no entity available for moving forward, the game moves on to the next team to play
        if (situation.first == EntityPickRule.NOT_MOVABLE && situation.second == null) {
            this.activeEntity = null
            this.lastEntityPickRule = null
            ingamePhase.phaseMode = GamePhaseMode.DICE
            ingamePhase.setNextControllingTeam()
            this.dicedNumber = null
            return
        }

        this.activeEntity = situation.second!!
        this.activeEntity!!.toggleHighlighting(true)
        this.lastEntityPickRule = situation.first
        ingamePhase.phaseMode = GamePhaseMode.MOVE_ENTITY
    }

    fun movePickedEntity() {
        if (this.dicedNumber == null) return
        if (this.activeEntity == null) return

        val currentFieldId: Int? = this.activeEntity!!.currentFieldId
        val teamStartPoint = 0

        this.activeEntity?.newGoalFieldId = if (currentFieldId == null) teamStartPoint + this.dicedNumber!! else currentFieldId + this.dicedNumber!!

        if (!this.activeEntity!!.shouldMove) this.activeEntity!!.shouldMove = true
        if (this.activeEntity!!.controller == null) this.activeEntity!!.controller = this
    }

    fun hasSavedAllEntities(): Boolean {
        return LudoGame.instance.gameEntityHandler.getEntitiesFromTeam(this.arenaId, this.teamName).all { it.isInGarage() && !it.isMovableTo(1) }
    }

    fun toBukkitInstance(): Player? {
        return Bukkit.getPlayer(this.uuid)
    }

}