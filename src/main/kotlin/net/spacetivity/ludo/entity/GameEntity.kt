package net.spacetivity.ludo.entity

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.field.GameField
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.team.GameTeam
import net.spacetivity.ludo.utils.MetadataUtils
import net.spacetivity.ludo.utils.PathFace
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity

data class GameEntity(val arenaId: String, val teamName: String, val entityType: EntityType, val entityId: Int) {

    var newGoalFieldId: Int? = null
    var currentFieldId: Int? = null
    var livingEntity: LivingEntity? = null

    var controller: GamePlayer? = null
    var shouldMove: Boolean = false

    var lastStartField: Int? = null

    private var forceYaw: Float? = null

    init {
        LudoGame.instance.gameEntityHandler.gameEntities.put(this.arenaId, this)
    }

    fun spawn(location: Location) {
        if (this.livingEntity != null) return

        this.livingEntity = location.world.spawnEntity(location, this.entityType) as LivingEntity
        this.livingEntity!!.isSilent = true;
        this.livingEntity!!.isInvulnerable = true
        this.livingEntity!!.setAI(false)
        this.livingEntity!!.isCustomNameVisible = true

        val gameTeam: GameTeam = LudoGame.instance.gameTeamHandler.getTeam(this.arenaId, this.teamName) ?: return
        this.livingEntity!!.customName(Component.text(this.teamName.uppercase(), gameTeam.color, TextDecoration.BOLD))
        MetadataUtils.apply(this.livingEntity!!, "teamName", this.teamName)
    }

    fun toggleHighlighting(active: Boolean) {
        this.livingEntity!!.isGlowing = active

        val gameTeam: GameTeam = LudoGame.instance.gameTeamHandler.getTeam(this.arenaId, this.teamName) ?: return
        if (active) gameTeam.scoreboardTeam.addEntity(this.livingEntity!!)
        else gameTeam.scoreboardTeam.removeEntity(this.livingEntity!!)
    }

    fun despawn() {
        if (this.livingEntity == null) return
        this.livingEntity!!.remove()
        this.livingEntity = null
    }

    fun landsAfterOpponent(dicedNumber: Int): Boolean { //TODO: test this!!!
        val startFieldId: Int = if (this.currentFieldId == null) 0 else this.currentFieldId!!
        val goalFieldId: Int = if (this.currentFieldId == null) 0 else this.currentFieldId!! + dicedNumber

        if (isTeamGarageField(startFieldId)) return false

        var isAfterOpponent = false

        for (currentFieldId: Int in startFieldId..goalFieldId) {
            val field: GameField = getTeamField(currentFieldId) ?: continue
            if (currentFieldId == goalFieldId) continue //if the field is the goal field

            if (field.isTaken && field.currentHolder?.teamName != this.teamName) isAfterOpponent = true
        }

        return isAfterOpponent
    }

    fun isInGarage(): Boolean {
        if (this.currentFieldId == null) return false
        return getTeamField(this.currentFieldId!!)!!.isGarageField
    }

    fun isAtSpawn(): Boolean {
        return this.currentFieldId == null
    }

    fun isGarageInSight(dicedNumber: Int): Boolean {
        if (this.currentFieldId == null) return false

        val startFieldId: Int = if (this.currentFieldId == null) 0 else this.currentFieldId!!
        val goalFieldId: Int = startFieldId + dicedNumber

        var containsGarageField = false

        for (currentFieldId: Int in startFieldId..goalFieldId) {
            val field: GameField = getTeamField(currentFieldId) ?: continue
            if (!field.isGarageField) continue
            containsGarageField = true
        }

        return containsGarageField
    }

    fun isMovableTo(dicedNumber: Int): Boolean {
        if (dicedNumber != 6 && this.currentFieldId == null)
            return false

        val goalFieldId: Int = if (this.currentFieldId == null) 0 else this.currentFieldId!! + dicedNumber

        val goalField: GameField = getTeamField(goalFieldId) ?: return false

        if (isBlockedByTeamMember(goalField)) {
            return false
        }

        val lastFieldForTeam: GameField = LudoGame.instance.gameFieldHandler.getLastFieldForTeam(this.arenaId, this.teamName)
            ?: throw NullPointerException("Last field cannot be found for team $teamName")
        val lastFieldId = lastFieldForTeam.properties.getFieldId(this.teamName)

        if (this.currentFieldId != null && this.currentFieldId == lastFieldId)
            return false

        return true
    }

    fun hasTargetAtGoalField(dicedNumber: Int): Boolean {
        val startFieldId: Int = if (this.currentFieldId == null) 0 else this.currentFieldId!!
        val goalFieldId: Int = startFieldId + dicedNumber

        if (isTeamGarageField(startFieldId)) return false

        val goalField: GameField = getTeamField(goalFieldId) ?: return false
        return goalField.isTaken && goalField.currentHolder?.teamName != this.teamName
    }

    fun moveOneFieldForward(dicedNumber: Int, fieldHeight: Double): Boolean {
        if (this.livingEntity == null) return false

        if (this.lastStartField == null) {
            if (this.currentFieldId == null)
                this.lastStartField = 0
            else
                this.lastStartField = this.currentFieldId
        }

        val newFieldId: Int = if (this.currentFieldId == null) 0 else this.currentFieldId!! + 1
        val goalFieldId: Int = if (this.currentFieldId == null) 0 else if (dicedNumber == 1) newFieldId else this.lastStartField!! + dicedNumber

        val goalField: GameField = getTeamField(goalFieldId) ?: return false
        val newField: GameField = getTeamField(newFieldId) ?: return false

        // clears holder from old field
        val oldField: GameField = getTeamField(this.currentFieldId!!)!!

        if (this.currentFieldId != null) {
            if (oldField.isTaken && (oldField.currentHolder != null && oldField.currentHolder?.teamName == this.teamName && oldField.currentHolder?.livingEntity?.uniqueId == this.livingEntity?.uniqueId)) {
                oldField.isTaken = false
                oldField.currentHolder = null
            }
        } else {
            LudoGame.instance.gameTeamHandler.getLocationOfTeam(this.arenaId, this.teamName, oldField.x, 0.0, oldField.z)?.isTaken = false
        }

        val rotation: PathFace? = newField.properties.rotation
        val teamEntranceName: String? = newField.properties.teamEntrance

        this.currentFieldId = newFieldId

        // decides if the entity needs to rotate (old statement in notepad...)
        if (rotation != null && (teamEntranceName == null || teamEntranceName == this.teamName))
            this.forceYaw = rotation.radians

        // checks if the current field has already a holder, if yes and the new field is not the goal field, the field will be skipped.
        if ((newFieldId != goalFieldId) && newField.isTaken)
            return false

        val worldPosition: Location = newField.getWorldPosition(fieldHeight)
        if (this.forceYaw != null) worldPosition.yaw = this.forceYaw!!
        this.livingEntity!!.teleport(worldPosition)

        val currentHolderTeamName: String = newField.currentHolder?.teamName ?: "-/-"

        // checks if the new field contains already a new entity. If 'yes' it throws the entity out.
        println(">> Moving (newFieldId: $newFieldId) (goalFieldId: $goalFieldId [${goalField.properties.getFieldId(teamName)}]) oldHolderTeam: $currentHolderTeamName | thisTeam: $teamName")

        if ((newFieldId == goalFieldId) && (newField.isTaken && currentHolderTeamName != this.teamName)) {
            println("Throws out old holder!")
            goalField.trowOutOldHolder(this.livingEntity!!)
        }

        newField.isTaken = true
        newField.currentHolder = this

        return this.currentFieldId == goalFieldId
    }

    private fun getTeamField(id: Int): GameField? {
        return LudoGame.instance.gameFieldHandler.getFieldForTeam(this.arenaId, this.teamName, id)
    }

    private fun isTeamGarageField(id: Int): Boolean {
        return getTeamField(id)?.isGarageField ?: false
    }

    private fun isBlockedByTeamMember(field: GameField): Boolean {
        return field.isTaken && field.currentHolder?.teamName == this.teamName
    }

}