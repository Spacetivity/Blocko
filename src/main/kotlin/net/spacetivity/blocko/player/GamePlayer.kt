package net.spacetivity.blocko.player

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.entity.GameEntityStatus
import net.spacetivity.blocko.entity.GameEntityType
import net.spacetivity.blocko.extensions.isDicing
import net.spacetivity.blocko.phase.GamePhaseMode
import net.spacetivity.blocko.phase.impl.IngamePhase
import net.spacetivity.blocko.scoreboard.GameScoreboardUtils
import net.spacetivity.blocko.stats.GamePlayerMatchStats
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

class GamePlayer(val uuid: UUID, val name: String, val arenaId: String, var teamName: String?, val isAI: Boolean) {

    val matchStats: GamePlayerMatchStats = GamePlayerMatchStats()

    var dicedNumber: Int? = null
    var activeEntity: GameEntity? = null
    var lastEntityPickRule: EntityPickRule? = null
    var actionTimeoutTimestamp: Long? = null
    var selectedEntityType: GameEntityType = GameEntityType.VILLAGER

    fun dice(ingamePhase: IngamePhase) {
        if (isDicing()) return
        BlockoGame.instance.diceHandler.startDicing(this, ingamePhase)
    }

    fun manuallyPickEntity(ingamePhase: IngamePhase, gameEntity: GameEntity) {
        if (this.dicedNumber == null) return
        this.activeEntity = gameEntity
        this.activeEntity!!.entityStatus = GameEntityStatus.MOVING
        this.actionTimeoutTimestamp = null
        BlockoGame.instance.bossbarHandler.unregisterBossbar(toBukkitInstance()!!, "timeoutBar")

        GameScoreboardUtils.updateEntityStatusLine(this.activeEntity!!)
        ingamePhase.phaseMode = GamePhaseMode.MOVE_ENTITY
    }

    fun autoPickEntity(ingamePhase: IngamePhase) {
        if (this.dicedNumber == null) return

        val situation: Pair<EntityPickRule, GameEntity?> = EntityPickRule.analyzeCurrentRuleSituation(this, this.dicedNumber!!)

        if (situation.first == EntityPickRule.NOT_MOVABLE && situation.second == null) {
            this.activeEntity = null
            this.lastEntityPickRule = null
            ingamePhase.phaseMode = GamePhaseMode.DICE
            ingamePhase.setNextControllingTeam()
            this.dicedNumber = null
            return
        }

        this.activeEntity = situation.second!!
        this.activeEntity!!.entityStatus = GameEntityStatus.MOVING
        this.activeEntity!!.toggleHighlighting(true)
        this.lastEntityPickRule = situation.first

        GameScoreboardUtils.updateEntityStatusLine(this.activeEntity!!)
        ingamePhase.phaseMode = GamePhaseMode.MOVE_ENTITY
    }

    fun movePickedEntity() {
        if (this.dicedNumber == null) return
        if (this.activeEntity == null) return

        val currentFieldId: Int? = this.activeEntity!!.currentFieldId
        val teamStartPoint = 0

        this.activeEntity?.newGoalFieldId = if (currentFieldId == null) teamStartPoint + this.dicedNumber!! else currentFieldId + this.dicedNumber!!

        val activeEntity1: GameEntity = this.activeEntity ?: throw NullPointerException("ACTIVE ENTITY IS NULL")

        if (!activeEntity1.shouldMove)
            this.activeEntity!!.shouldMove = true

        if (this.activeEntity!!.controller == null) this.activeEntity!!.controller = this
    }

    fun hasSavedAllEntities(): Boolean {
        return BlockoGame.instance.gameEntityHandler.getEntitiesFromTeam(this.arenaId, this.teamName!!).all { it.isInGarage() && !it.isMovableTo(1) }
    }

    fun toBukkitInstance(): Player? {
        return Bukkit.getPlayer(this.uuid)
    }

}