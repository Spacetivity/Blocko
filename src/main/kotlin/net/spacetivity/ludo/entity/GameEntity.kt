package net.spacetivity.ludo.entity

import net.kyori.adventure.text.Component
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.field.GameField
import net.spacetivity.ludo.field.GameFieldHandler
import net.spacetivity.ludo.garageField.GameGarageField
import net.spacetivity.ludo.garageField.GameGarageFieldHandler
import net.spacetivity.ludo.team.GameTeam
import net.spacetivity.ludo.utils.BoardField
import net.spacetivity.ludo.utils.MetadataUtils
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity

data class GameEntity(val arenaId: String, val teamName: String, val entityType: EntityType) {

    var currentFieldId: Int? = null
    var livingEntity: LivingEntity? = null

    private var forceYaw: Float? = null

    fun spawn(location: Location) {
        if (this.livingEntity != null) return

        val gameTeam: GameTeam = LudoGame.instance.gameTeamHandler.getTeam(this.arenaId, this.teamName) ?: return

        this.livingEntity = location.world.spawnEntity(location, this.entityType) as LivingEntity
        this.livingEntity!!.isSilent = true;
        this.livingEntity!!.isGlowing = true
        this.livingEntity!!.isInvulnerable = true
        this.livingEntity!!.setAI(false)

        gameTeam.scoreboardTeam.addEntity(this.livingEntity!!)
        MetadataUtils.set(this.livingEntity!!, "teamName", this.teamName)
    }

    fun despawn() {
        if (this.livingEntity == null) return
        this.livingEntity!!.remove()
        this.livingEntity = null
    }

    fun move(dicedNumber: Int, fieldHeight: Double) {
        if (this.livingEntity == null) return

        val gameFieldHandler: GameFieldHandler = LudoGame.instance.gameFieldHandler

        if (this.currentFieldId != null) {
            val oldField: GameField = gameFieldHandler.getField(this.arenaId, this.currentFieldId!!) ?: return
            oldField.isTaken = false

            var continueMoveFunc = true

            if (MetadataUtils.has(this.livingEntity!!, "inGarage") || (oldField.teamGarageEntrance != null && oldField.teamGarageEntrance.equals(this.teamName, true))) {
                if (!MetadataUtils.has(this.livingEntity!!, "inGarage")) this.currentFieldId = null
                MetadataUtils.setIfAbsent(this.livingEntity!!, "inGarage", this.teamName)
                moveInGarage(fieldHeight, dicedNumber)
                continueMoveFunc = false
            }

            if (!continueMoveFunc)
                return
        }

        val teamStartFieldId = 0
        val newFieldId: Int = if (this.currentFieldId == null) teamStartFieldId else this.currentFieldId!! + 1
        val goalFieldId: Int = if (dicedNumber == 1 || this.currentFieldId == null) newFieldId else this.currentFieldId!! + dicedNumber
        val goalField: GameField = gameFieldHandler.getField(this.arenaId, goalFieldId) ?: return

        this.currentFieldId = newFieldId

        val newField: GameField = gameFieldHandler.getField(this.arenaId, this.currentFieldId!!) ?: return

        if (isBlocked(newField) || isBlocked(goalField)) {
            val gameArena: GameArena = LudoGame.instance.gameArenaHandler.getArena(this.arenaId) ?: return
            gameArena.sendArenaMessage(Component.text("Cannot move entity. Target field is blocked!"))
            return
        }

        // checks if the new field contains already a new entity. If 'yes' it throws the entity out.
        newField.checkForOpponent(this.livingEntity!!)
        newField.isTaken = true

        val worldPosition: Location = newField.getWorldPosition(fieldHeight)

        if (newField.turnComponent != null) {
            val teamEntranceName: String? = newField.teamGarageEntrance
            val isTurnAllowed: Boolean = teamEntranceName.equals(this.teamName, true) || teamEntranceName == null
            if (isTurnAllowed) this.forceYaw = newField.turnComponent!!.getRotation()
        }

        if (this.forceYaw != null) worldPosition.yaw = this.forceYaw!!
        this.livingEntity!!.teleport(worldPosition)
    }

    private fun moveInGarage(fieldHeight: Double, dicedNumber: Int) {
        if (this.livingEntity == null) return

        val gameGarageFieldHandler: GameGarageFieldHandler = LudoGame.instance.gameGarageFieldHandler

        val newFieldId: Int = if (this.currentFieldId == null) 0 else this.currentFieldId!! + 1
        val newField: GameGarageField = gameGarageFieldHandler.getGarageField(this.arenaId, this.teamName, newFieldId)
            ?: return

        this.currentFieldId = newFieldId

        val gameArena: GameArena = LudoGame.instance.gameArenaHandler.getArena(this.arenaId) ?: return

        if (dicedNumber > 4) {
            gameArena.sendArenaMessage(Component.text("You cannot move more than 4 fields in your garage!"))
            return
        }

        val goalFieldId: Int = if (dicedNumber == 1 || this.currentFieldId == null) newFieldId else this.currentFieldId!! + dicedNumber
        val goalField: GameGarageField = gameGarageFieldHandler.getGarageField(this.arenaId, this.teamName, goalFieldId) ?: return

        if (isBlocked(newField) || isBlocked(goalField)) {
            gameArena.sendArenaMessage(Component.text("Cannot move entity. Target field is blocked!"))
            return
        }

        newField.isTaken = true

        val worldPosition: Location = newField.getWorldPosition(fieldHeight)
        this.livingEntity!!.teleport(worldPosition)
    }

    private fun isBlocked(field: BoardField): Boolean {
        return field.isTaken && field.getCurrentHolder()!!.teamName == this.teamName
    }

}