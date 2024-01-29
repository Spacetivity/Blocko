package net.spacetivity.ludo.player

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.entity.GameEntity
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

class GamePlayer(val uuid: UUID, val arenaId: String, val teamName: String, val isAI: Boolean, var dicedNumber: Int?) {

    private var activeEntity: GameEntity? = null
    var inAction: Boolean = false

    fun dice() {
        if (inAction) return
    }

    fun pickEntity() {
        if (inAction) return
        if (this.dicedNumber == null) return
        inAction = true
        val gameEntities: List<GameEntity> = LudoGame.instance.gameEntityHandler.getEntitiesFromTeam(this.arenaId, this.teamName)
        val gameEntity: GameEntity = gameEntities.firstOrNull { it.isMovable(this.dicedNumber!!) } ?: return
        this.activeEntity = gameEntity
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