package net.spacetivity.blocko.entity

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.field.PathFace
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.scoreboard.GameScoreboardUtils
import net.spacetivity.blocko.team.GameTeam
import net.spacetivity.blocko.team.GameTeamLocation
import net.spacetivity.blocko.utils.LocationUtils
import net.spacetivity.blocko.utils.MetadataUtils
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Animals
import org.bukkit.entity.LivingEntity

data class GameEntity(val arenaId: String, val teamName: String, val gameEntityType: GameEntityType, val entityId: Int) {

    var newGoalFieldId: Int? = null
    var currentFieldId: Int? = null
    var livingEntity: LivingEntity? = null

    var controller: GamePlayer? = null
    var shouldMove: Boolean = false

    var lastStartField: Int? = null
    var isHighlighted = false

    var entityStatus: GameEntityStatus = GameEntityStatus.AT_SPAWN

    private var forceYaw: Float? = null

    init {
        BlockoGame.instance.gameEntityHandler.gameEntities.put(this.arenaId, this)
    }

    fun spawn(location: Location) {
        if (this.livingEntity != null) return

        this.livingEntity = location.world.spawnEntity(location, this.gameEntityType.bukkitEntityType) as LivingEntity
        this.livingEntity!!.isSilent = true
        this.livingEntity!!.isInvulnerable = true
        this.livingEntity!!.setAI(false)
        this.livingEntity!!.isCustomNameVisible = true

        if (this.gameEntityType.isBaby && this.livingEntity!! is Animals) {
            val animal: Animals = this.livingEntity as Animals
            animal.ageLock = true
            animal.setBaby()
        }

        val gameTeam: GameTeam = BlockoGame.instance.gameTeamHandler.getTeam(this.arenaId, this.teamName) ?: return
        this.livingEntity!!.customName(Component.text(this.teamName.uppercase(), gameTeam.color, TextDecoration.BOLD))
        MetadataUtils.apply(this.livingEntity!!, "teamName", this.teamName)
    }

    fun toggleHighlighting(active: Boolean) {
        if (this.isHighlighted == active) return
        if (this.livingEntity == null) return

        this.livingEntity?.isGlowing = active
        this.isHighlighted = active
    }

    fun despawn() {
        if (this.livingEntity == null) return
        this.livingEntity!!.remove()
        this.livingEntity = null
    }

    fun landsAfterOpponent(dicedNumber: Int): Boolean {
        val startFieldId: Int = if (this.currentFieldId == null) 0 else this.currentFieldId!!
        val goalFieldId: Int = if (this.currentFieldId == null) 0 else this.currentFieldId!! + dicedNumber

        if (isTeamGarageField(startFieldId)) return false

        var isAfterOpponent = false

        for (currentFieldId: Int in startFieldId..goalFieldId) {
            val field: GameField = getTeamField(currentFieldId) ?: continue
            if (currentFieldId == goalFieldId) continue

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
        if (dicedNumber != 6 && this.currentFieldId == null) return false

        val goalFieldId: Int = if (this.currentFieldId == null) 0 else this.currentFieldId!! + dicedNumber
        val goalField: GameField = getTeamField(goalFieldId) ?: return false
        if (goalField.isTaken && goalField.currentHolder?.teamName == this.teamName) return false

        val lastFieldForTeam: GameField = BlockoGame.instance.gameFieldHandler.getLastFieldForTeam(this.arenaId, this.teamName)
            ?: throw NullPointerException("Last field cannot be found for team $teamName")

        return !(this.currentFieldId != null && this.currentFieldId == lastFieldForTeam.properties.getFieldId(this.teamName))
    }

    fun hasTargetAtGoalField(dicedNumber: Int): Boolean {
        val startFieldId: Int = if (this.currentFieldId == null) 0 else this.currentFieldId!!
        val goalFieldId: Int = startFieldId + dicedNumber

        if (isTeamGarageField(startFieldId)) return false

        val goalField: GameField = getTeamField(goalFieldId) ?: return false
        return goalField.isTaken && goalField.currentHolder?.teamName != this.teamName
    }

    fun moveOneFieldForward(dicedNumber: Int): Boolean {
        if (this.livingEntity == null) return false

        if (this.lastStartField == null) {
            if (this.currentFieldId == null) this.lastStartField = 0
            else this.lastStartField = this.currentFieldId
        }

        val newFieldId: Int = if (this.currentFieldId == null) 0 else this.currentFieldId!! + 1
        val goalFieldId: Int = if (this.currentFieldId == null) 0 else if (dicedNumber == 1) newFieldId else this.lastStartField!! + dicedNumber

        val goalField: GameField = getTeamField(goalFieldId) ?: return false
        val newField: GameField = getTeamField(newFieldId) ?: return false

        val gameArena: GameArena = BlockoGame.instance.gameArenaHandler.getArena(this.arenaId)!!
        gameArena.sendArenaSound(Sound.BLOCK_BONE_BLOCK_STEP, 1.0F)

        if (this.currentFieldId != null) {
            val oldField: GameField = getTeamField(this.currentFieldId!!)!!
            if (oldField.isTaken && (oldField.currentHolder != null && oldField.currentHolder?.teamName == this.teamName && oldField.currentHolder?.livingEntity?.uniqueId == this.livingEntity?.uniqueId)) {
                oldField.isTaken = false
                oldField.currentHolder = null
            }
        } else {
            val location: Location = LocationUtils.centerLocation(this.livingEntity!!.location)
            val spawnLocation: GameTeamLocation = BlockoGame.instance.gameTeamHandler.getLocationOfTeam(this.arenaId, this.teamName, location.x, location.y, location.z)
                ?: throw NullPointerException("Spawn location of team ${this.teamName} is not found!")

            spawnLocation.isTaken = false
        }

        val rotation: PathFace? = newField.properties.rotation
        val teamEntranceName: String? = newField.properties.teamEntrance

        this.currentFieldId = newFieldId

        if (rotation != null && (teamEntranceName == null || teamEntranceName == this.teamName))
            this.forceYaw = rotation.radians

        if ((newFieldId != goalFieldId) && newField.isTaken)
            return false

        val worldPosition: Location = newField.getWorldPosition()
        if (this.forceYaw != null) worldPosition.yaw = this.forceYaw!!

        this.livingEntity!!.teleport(worldPosition)

        val currentHolderTeamName: String = newField.currentHolder?.teamName ?: "-/-"

        if ((newFieldId == goalFieldId) && (newField.isTaken && currentHolderTeamName != this.teamName))
            goalField.trowOutOldHolder(controller!!, this.livingEntity!!)

        newField.isTaken = true
        newField.currentHolder = this

        val reachedGoal: Boolean = this.currentFieldId == goalFieldId

        if (reachedGoal) {
            if (goalField.isGarageField) this.entityStatus = GameEntityStatus.SAVED
            else this.entityStatus = GameEntityStatus.ON_FIELD
            GameScoreboardUtils.updateEntityStatusLine(this)
        }

        return reachedGoal
    }

    private fun getTeamField(id: Int): GameField? {
        return BlockoGame.instance.gameFieldHandler.getFieldForTeam(this.arenaId, this.teamName, id)
    }

    private fun isTeamGarageField(id: Int): Boolean {
        return getTeamField(id)?.isGarageField ?: false
    }

}