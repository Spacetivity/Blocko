package net.spacetivity.ludo.entity

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.field.GameField
import net.spacetivity.ludo.field.GameFieldHandler
import net.spacetivity.ludo.team.GameTeam
import net.spacetivity.ludo.utils.MetadataUtils
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity

data class GameEntity(val arenaId: String, val teamName: String, val entityType: EntityType) {

    var currentFieldId: Int? = null
    var livingEntity: LivingEntity? = null

    fun spawn(location: Location) {
        if (this.livingEntity != null) return

        val gameTeam: GameTeam = LudoGame.instance.gameTeamHandler.getTeam(this.arenaId, this.teamName) ?: return

        this.livingEntity = location.world.spawnEntity(location, this.entityType) as LivingEntity
        this.livingEntity!!.isSilent = true;
        this.livingEntity!!.isGlowing = true
        this.livingEntity!!.isInvisible = true
        this.livingEntity!!.isInvulnerable = true
        this.livingEntity!!.setAI(false)

        gameTeam.scoreboardTeam?.addEntity(this.livingEntity!!)

        MetadataUtils.set(this.livingEntity!!, "teamName", this.teamName)
    }

    // field amount -> how many fields the entity should move (is decided with rolling the dice)
    fun move(fieldAmount: Int, fieldHeight: Double) {
        if (this.livingEntity == null) return

        val gameFieldHandler: GameFieldHandler = LudoGame.instance.gameFieldHandler

        if (this.currentFieldId != null) {
            val oldField: GameField = gameFieldHandler.getField(this.arenaId, this.currentFieldId!!) ?: return
            oldField.isTaken = false
        }

        val newFieldId: Int = if (this.currentFieldId == null) fieldAmount else this.currentFieldId!! + fieldAmount
        this.currentFieldId = newFieldId

        val newField: GameField = gameFieldHandler.getField(this.arenaId,this.currentFieldId!!) ?: return
        newField.isTaken = true

        val worldPosition: Location = newField.getWorldPosition(fieldHeight)

        if (newField.turnComponent != null)
            worldPosition.setDirection(newField.turnComponent!!.getRotation(this.livingEntity!!))

        this.livingEntity!!.teleport(newField.getWorldPosition(fieldHeight))
    }

    fun despawn() {
        if (this.livingEntity == null) return
        this.livingEntity!!.remove()
        this.livingEntity = null
    }

}