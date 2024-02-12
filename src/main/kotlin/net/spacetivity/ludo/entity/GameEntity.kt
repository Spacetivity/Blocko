package net.spacetivity.ludo.entity

import net.kyori.adventure.text.Component
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.field.GameField
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.team.GameTeam
import net.spacetivity.ludo.utils.MetadataUtils
import net.spacetivity.ludo.utils.PathFace
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

    init {
        LudoGame.instance.gameEntityHandler.gameEntities.put(this.arenaId, this)
    }

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
        val startFieldId: Int = if (this.currentFieldId == null) 0 else this.currentFieldId!!
        val goalFieldId: Int = startFieldId + dicedNumber

        if (isTeamGarageField(startFieldId)) return false

        var isAfterOpponent = false

        for (currentFieldId: Int in startFieldId..goalFieldId) {
            val field: GameField = getTeamField(currentFieldId) ?: continue
            if (currentFieldId == goalFieldId) continue //if the field is the goal field

            if (field.isTaken && field.getCurrentHolder()!!.teamName != this.teamName) isAfterOpponent = true
        }

        return isAfterOpponent
    }

    fun isInGarage(): Boolean {
        if (this.currentFieldId == null) return false
        return getTeamField(this.currentFieldId!!)!!.isGarageField
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

    fun isMovable(dicedNumber: Int): Boolean {
        val startFieldId: Int = if (this.currentFieldId == null) 0 else this.currentFieldId!!
        val goalFieldId: Int = startFieldId + dicedNumber

        val goalField: GameField = getTeamField(goalFieldId) ?: return false

        if (isBlockedByTeamMember(goalField))
            return false

        if (startFieldId == LudoGame.instance.gameFieldHandler.getLastFieldForTeam(this.arenaId, this.teamName).properties.getFieldId(this.teamName))
            return false

        return true
    }

    fun hasTargetAtGoalField(dicedNumber: Int): Boolean {
        val startFieldId: Int = if (this.currentFieldId == null) 0 else this.currentFieldId!!
        val goalFieldId: Int = startFieldId + dicedNumber

        if (isTeamGarageField(startFieldId)) return false

        val goalField: GameField = getTeamField(goalFieldId) ?: return false
        return goalField.isTaken && goalField.getCurrentHolder()!!.teamName != this.teamName
    }

    fun moveOneFieldForward(dicedNumber: Int, fieldHeight: Double): Boolean {
        if (this.livingEntity == null) return false

        if (this.currentFieldId != null) {
            val oldField: GameField = getTeamField(this.currentFieldId!!)!!
            oldField.isTaken = false
        }

        val teamStartFieldId = 0

        val newFieldId: Int = if (this.currentFieldId == null) teamStartFieldId else this.currentFieldId!! + 1

        val goalFieldId: Int = if (dicedNumber == 1 || this.currentFieldId == null) newFieldId else this.currentFieldId!! + dicedNumber
        val goalField: GameField = getTeamField(goalFieldId) ?: return false

        this.currentFieldId = newFieldId

        val newField: GameField = getTeamField(this.currentFieldId!!) ?: return false

        if (isBlockedByTeamMember(newField) || isBlockedByTeamMember(goalField)) {
            val gameArena: GameArena = LudoGame.instance.gameArenaHandler.getArena(this.arenaId) ?: return false
            gameArena.sendArenaMessage(Component.text("Cannot move entity. Target field is blocked!"))
            return false
        }

        // checks if the new field contains already a new entity. If 'yes' it throws the entity out.
        newField.checkForOpponent(this.livingEntity!!)
        newField.isTaken = true

        val worldPosition: Location = newField.getWorldPosition(fieldHeight)

        val rotation: PathFace? = newField.properties.rotation

        if (rotation != null) {
            val teamEntranceName: String? = newField.properties.teamEntrance
            val isTurnAllowed: Boolean = teamEntranceName.equals(this.teamName, true) || teamEntranceName == null
            if (isTurnAllowed) this.forceYaw = rotation.radians
        }

        if (this.forceYaw != null) worldPosition.yaw = this.forceYaw!!
        this.livingEntity!!.teleport(worldPosition)

        return this.newGoalFieldId == goalFieldId
    }

    private fun getTeamField(id: Int): GameField? {
        return LudoGame.instance.gameFieldHandler.getFieldForTeam(this.arenaId, this.teamName, id)
    }

    private fun isTeamGarageField(id: Int): Boolean {
        return getTeamField(id)?.isGarageField ?: false
    }

    private fun isBlockedByTeamMember(field: GameField): Boolean {
        return field.isTaken && field.getCurrentHolder()!!.teamName == this.teamName
    }

}