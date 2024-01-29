package net.spacetivity.ludo.player

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.entity.GameEntity
import net.spacetivity.ludo.extensions.startDicing
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

class GamePlayer(val uuid: UUID, val arenaId: String, val teamName: String, val isAI: Boolean, var dicedNumber: Int?) {

    private var activeEntity: GameEntity? = null
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
        val gameEntities: List<GameEntity> = LudoGame.instance.gameEntityHandler.getEntitiesFromTeam(this.arenaId, this.teamName)

        var resultEntity: GameEntity? = null

        for (gameEntity in gameEntities) {
            if (gameEntity.isMovable(this.dicedNumber!!)) {
                resultEntity = gameEntity
            } else if (gameEntity.isMovable(this.dicedNumber!!) && gameEntity.hasValidTarget(this.dicedNumber!!)) {
                resultEntity = gameEntity
            }
        }

        if (resultEntity == null) return

        this.activeEntity = resultEntity
        inAction = false
    }

    fun movePickedEntity(fieldHeight: Double) {
        if (inAction) return
        if (this.dicedNumber == null) return
        if (this.activeEntity == null) return

        inAction = true
        this.activeEntity!!.move(this.dicedNumber!!, fieldHeight)
    }

    fun hasWon(): Boolean {
        return LudoGame.instance.gameEntityHandler.getEntitiesFromTeam(this.arenaId, this.teamName).all { !it.isMovable(1) }
    }

    fun toBukkitInstance(): Player? {
        return Bukkit.getPlayer(this.uuid)
    }

}