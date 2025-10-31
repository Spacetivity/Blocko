package net.spacetivity.blocko.player

import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.arena.id.ArenaId
import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.entity.GameEntityStatus
import net.spacetivity.blocko.entity.GameEntityType
import net.spacetivity.blocko.phase.GamePhaseMode
import net.spacetivity.blocko.phase.impl.IngamePhase
import net.spacetivity.blocko.stats.GamePlayerMatchStats
import net.spacetivity.blocko.utils.Constants
import net.spacetivity.blocko.utils.ScoreboardUtils
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

class GamePlayer(val uuid: UUID, val name: String, val arenaId: ArenaId, var teamName: String?, val isAI: Boolean) {

    val matchStats = GamePlayerMatchStats()

    var dicedNumber: Int? = null
    var activeEntity: GameEntity? = null
    var lastEntityPickRule: EntityPickRule? = null
    var actionTimeoutTimestamp: Long? = null

    var selectedEntityType: GameEntityType = if (isAI) {
        GameEntityType.entries.random()
    } else {
        Blocko.instance.gameEntityHandler.getSelectedEntityType(this.uuid)
    }

    fun dice(ingamePhase: IngamePhase) {
        if (isDicing()) return
        Blocko.instance.diceHandler.startDicing(this, ingamePhase)
    }

    fun manuallyPickEntity(ingamePhase: IngamePhase, gameEntity: GameEntity) {
        if (this.dicedNumber == null) return
        this.activeEntity = gameEntity
        this.activeEntity!!.entityStatus = GameEntityStatus.MOVING
        this.actionTimeoutTimestamp = null

        for (gamePlayer in Blocko.instance.arenaHandler.getArena(this.arenaId)!!.currentPlayers.filter { !it.isAI }) {
            Blocko.instance.bossbarHandler.unregisterBossbar(gamePlayer.toBukkitInstance()!!, Constants.TIMEOUT_BOSSBAR_NAME)
        }

        ScoreboardUtils.updateEntityStatusLine(this.activeEntity!!)
        ingamePhase.phaseMode = GamePhaseMode.MOVE_ENTITY
    }

    fun autoPickEntity(ingamePhase: IngamePhase) {
        if (this.dicedNumber == null) return

        val situation = Blocko.instance.aiEntityHandler.analyzeSituation(this, this.dicedNumber!!)
        this.actionTimeoutTimestamp = null

        if (situation.rule == EntityPickRule.NOT_MOVABLE && situation.selectedEntity == null) {
            this.activeEntity = null
            this.lastEntityPickRule = null
            ingamePhase.phaseMode = GamePhaseMode.DICE
            ingamePhase.setNextControllingTeam()
            this.dicedNumber = null
            return
        }

        this.activeEntity = situation.selectedEntity!!
        this.activeEntity!!.entityStatus = GameEntityStatus.MOVING
        this.activeEntity!!.toggleHighlighting(true)
        this.lastEntityPickRule = situation.rule

        for (gamePlayer in Blocko.instance.arenaHandler.getArena(this.arenaId)!!.currentPlayers.filter { !it.isAI }) {
            Blocko.instance.bossbarHandler.unregisterBossbar(gamePlayer.toBukkitInstance()!!, Constants.TIMEOUT_BOSSBAR_NAME)
        }

        ScoreboardUtils.updateEntityStatusLine(this.activeEntity!!)
        ingamePhase.phaseMode = GamePhaseMode.MOVE_ENTITY
    }

    fun movePickedEntity() {
        if (this.dicedNumber == null) return
        if (this.activeEntity == null) return

        val currentFieldId = this.activeEntity!!.currentFieldId
        val teamStartPoint = 0

        this.activeEntity?.newGoalFieldId = if (currentFieldId == null) teamStartPoint + this.dicedNumber!! else currentFieldId + this.dicedNumber!!

        val activeEntity1 = this.activeEntity ?: throw NullPointerException("Active entity is null")

        if (!activeEntity1.shouldMove)
            this.activeEntity!!.shouldMove = true

        if (this.activeEntity!!.controller == null) this.activeEntity!!.controller = this
    }

    fun hasSavedAllEntities(): Boolean {
        return Blocko.instance.gameEntityHandler.getEntitiesFromTeam(this.arenaId, this.teamName!!).all { it.isInGarage() && !it.isMovableTo(1) }
    }

    fun toBukkitInstance(): Player? {
        return Bukkit.getPlayer(this.uuid)
    }

}