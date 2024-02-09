package net.spacetivity.ludo.entity

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.team.GameTeam
import net.spacetivity.ludo.utils.BoardField
import net.spacetivity.ludo.utils.MetadataUtils
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity

data class GameEntity(val arenaId: String, val teamName: String, val entityType: EntityType) {

    var newGoalFieldId: Int? = null
    var currentFieldId: Int? = null
    var livingEntity: LivingEntity? = null

    private var forceYaw: Float? = null

    var controller: GamePlayer? = null
    var shouldMove: Boolean = false

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

    fun landsAfterOpponent(dicedNumber: Int): Boolean {
        return false
//        if (MetadataUtils.has(this.livingEntity!!, "inGarage")) return false
//
//        //check if there is an opponent in the next fields until the
//
//        val startFieldId: Int = if (this.currentFieldId == null) 0 else this.currentFieldId!!
//        val goalFieldId: Int = startFieldId + dicedNumber
//
//        var isAfterOpponent = false
//
//        for (currentFieldId: Int in startFieldId..startFieldId + dicedNumber) {
//            val field: GameField = LudoGame.instance.gameFieldHandler.getField(this.arenaId, currentFieldId) ?: continue
//            if (field.id == goalFieldId) continue //if the field is the goal field
//
//            if (field.isTaken && field.getCurrentHolder()!!.teamName != this.teamName) isAfterOpponent = true
//        }
//
//        return isAfterOpponent
    }

    fun isGarageInSight(dicedNumber: Int): Boolean {
        if (this.currentFieldId == null) return false

        var fieldsTravelled = 0

//        for (currentFieldId: Int in this.currentFieldId!!..this.currentFieldId!! + dicedNumber) {
//            val field: GameField = LudoGame.instance.gameFieldHandler.getField(this.arenaId, currentFieldId) ?: continue
//            if (field.teamGarageEntrance == null) continue
//            if (field.teamGarageEntrance != this.teamName) continue
//
//            fieldsTravelled++
//            break
//        }

        return (dicedNumber - 1) > fieldsTravelled
    }

    // wenn innerhalb der gewürfelten reichweite ein garagen feld ist, lass movable bis genau zum eingang true sein
    fun isMovable(dicedNumber: Int): Boolean {
        return true
//        val gameGarageFieldHandler: GameGarageFieldHandler = LudoGame.instance.gameGarageFieldHandler
//        val gameFieldHandler: GameFieldHandler = LudoGame.instance.gameFieldHandler
//
//        var teamEntrancePresent: Boolean = false
//        var verbraucheWürfelZüge: Int = 0
//
//        for (i in this.currentFieldId!! .. this.currentFieldId!! + dicedNumber) {
//            val field: GameField = gameFieldHandler.getField(this.arenaId, i) ?: continue
//            verbraucheWürfelZüge++
//            if (field.teamGarageEntrance == null) continue
//            teamEntrancePresent = true
//        }
//
//        if (MetadataUtils.has(this.livingEntity!!, "inGarage")) {
//            val goalGarageField: GameGarageField = gameGarageFieldHandler.getGarageField(this.arenaId, this.teamName, this.currentFieldId!! + dicedNumber)
//                ?: return false
//            return !goalGarageField.isTaken
//        } else if (teamEntrancePresent) {
//            val correctedDicedNum = dicedNumber - verbraucheWürfelZüge
//            val goalField: GameField = gameFieldHandler.getField(this.arenaId, this.currentFieldId!! + correctedDicedNum)
//                ?: return false
//            return !(goalField.isTaken && goalField.getCurrentHolder()!!.teamName == this.teamName)
//        } else {
//            val goalField: GameField = gameFieldHandler.getField(this.arenaId, this.currentFieldId!! + dicedNumber)
//                ?: return false
//            return !(goalField.isTaken && goalField.getCurrentHolder()!!.teamName == this.teamName)
//        }
    }

    fun hasValidTarget(dicedNumber: Int): Boolean {
        return false
//        if (MetadataUtils.has(this.livingEntity!!, "inGarage")) return false
//        val gameFieldHandler: GameFieldHandler = LudoGame.instance.gameFieldHandler
//
//        val goalField: GameField = gameFieldHandler.getField(this.arenaId, this.currentFieldId!! + dicedNumber)
//            ?: return false
//        return goalField.isTaken && goalField.getCurrentHolder()!!.teamName != this.teamName
    }

    fun move(dicedNumber: Int, fieldHeight: Double): Boolean {
        return false
//        if (this.livingEntity == null) return false
//
//        val gameFieldHandler: GameFieldHandler = LudoGame.instance.gameFieldHandler
//
//        if (this.currentFieldId != null) {
//            val oldField: GameField = gameFieldHandler.getField(this.arenaId, this.currentFieldId!!) ?: return false
//            oldField.isTaken = false
//
//            if (MetadataUtils.has(this.livingEntity!!, "inGarage") || (oldField.teamGarageEntrance != null && oldField.teamGarageEntrance.equals(this.teamName, true))) {
//                if (!MetadataUtils.has(this.livingEntity!!, "inGarage")) this.currentFieldId = null
//                MetadataUtils.setIfAbsent(this.livingEntity!!, "inGarage", this.teamName)
//                return moveInGarage(fieldHeight, dicedNumber)
//            }
//        }
//
//        val teamStartFieldId = 0
//        val newFieldId: Int = if (this.currentFieldId == null) teamStartFieldId else this.currentFieldId!! + 1
//        val goalFieldId: Int = if (dicedNumber == 1 || this.currentFieldId == null) newFieldId else this.currentFieldId!! + dicedNumber
//        val goalField: GameField = gameFieldHandler.getField(this.arenaId, goalFieldId) ?: return false
//
//        this.currentFieldId = newFieldId
//
//        val newField: GameField = gameFieldHandler.getField(this.arenaId, this.currentFieldId!!) ?: return false
//
//        if (isBlocked(newField) || isBlocked(goalField)) {
//            val gameArena: GameArena = LudoGame.instance.gameArenaHandler.getArena(this.arenaId) ?: return false
//            gameArena.sendArenaMessage(Component.text("Cannot move entity. Target field is blocked!"))
//            return false
//        }
//
//        // checks if the new field contains already a new entity. If 'yes' it throws the entity out.
//        newField.checkForOpponent(this.livingEntity!!)
//        newField.isTaken = true
//
//        val worldPosition: Location = newField.getWorldPosition(fieldHeight)
//
//        if (newField.turnComponent != null) {
//            val teamEntranceName: String? = newField.teamGarageEntrance
//            val isTurnAllowed: Boolean = teamEntranceName.equals(this.teamName, true) || teamEntranceName == null
//            if (isTurnAllowed) this.forceYaw = newField.turnComponent!!.getRotation()
//        }
//
//        if (this.forceYaw != null) worldPosition.yaw = this.forceYaw!!
//        this.livingEntity!!.teleport(worldPosition)
//
//        return this.newGoalFieldId == goalFieldId
    }

    private fun moveInGarage(fieldHeight: Double, dicedNumber: Int): Boolean {
        return false
//        if (this.livingEntity == null) return false
//
//        val gameGarageFieldHandler: GameGarageFieldHandler = LudoGame.instance.gameGarageFieldHandler
//
//        val newFieldId: Int = if (this.currentFieldId == null) 0 else this.currentFieldId!! + 1
//        val newField: GameGarageField = gameGarageFieldHandler.getGarageField(this.arenaId, this.teamName, newFieldId)
//            ?: return false
//
//        this.currentFieldId = newFieldId
//
//        val gameArena: GameArena = LudoGame.instance.gameArenaHandler.getArena(this.arenaId) ?: return false
//
//        if (dicedNumber > 4) {
//            gameArena.sendArenaMessage(Component.text("You cannot move more than 4 fields in your garage!"))
//            return false
//        }
//
//        val goalFieldId: Int = if (dicedNumber == 1 || this.currentFieldId == null) newFieldId else this.currentFieldId!! + dicedNumber
//        val goalField: GameGarageField = gameGarageFieldHandler.getGarageField(this.arenaId, this.teamName, goalFieldId)
//            ?: return false
//
//        if (isBlocked(newField) || isBlocked(goalField)) {
//            gameArena.sendArenaMessage(Component.text("Cannot move entity. Target field is blocked!"))
//            return false
//        }
//
//        newField.isTaken = true
//
//        val worldPosition: Location = newField.getWorldPosition(fieldHeight)
//        this.livingEntity!!.teleport(worldPosition)
//
//        return this.newGoalFieldId == goalFieldId
    }

    private fun isBlocked(field: BoardField): Boolean {
        return field.isTaken && field.getCurrentHolder()!!.teamName == this.teamName
    }

}