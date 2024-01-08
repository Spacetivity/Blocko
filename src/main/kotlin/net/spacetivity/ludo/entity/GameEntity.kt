package net.spacetivity.ludo.entity

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.board.GameBoard
import net.spacetivity.ludo.board.GameField
import net.spacetivity.ludo.team.GameTeam
import net.spacetivity.ludo.utils.ItemUtils
import net.spacetivity.ludo.utils.MetadataUtils
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity

data class GameEntity(val arenaId: String, val teamName: String, val entityType: EntityType) {

    var currentFieldId: Int? = null
    var livingEntity: LivingEntity? = null

    fun spawn(location: Location) {
        if (this.livingEntity != null) return

        val gameArena: GameArena = LudoGame.instance.gameArenaHandler?.getArena(this.arenaId)?: return
        val gameTeam: GameTeam = gameArena.gameTeamHandler.getTeam(this.teamName) ?: return

        this.livingEntity = location.world.spawnEntity(location, this.entityType) as LivingEntity
        this.livingEntity!!.isSilent = true;
        this.livingEntity!!.isGlowing = true
        this.livingEntity!!.isInvisible = true
        this.livingEntity!!.setGravity(false)
        this.livingEntity!!.setAI(false)

        if (this.livingEntity is ArmorStand) {
            val armorStand: ArmorStand = this.livingEntity as ArmorStand
            armorStand.setBasePlate(false)
            armorStand.setArms(false)
            armorStand.isSmall = true
            armorStand.equipment.helmet = ItemUtils(Material.PLAYER_HEAD).setOwner(gameTeam.headValue).build()
        }

        gameTeam.scoreboardTeam?.addEntity(this.livingEntity!!)

        MetadataUtils.set(this.livingEntity!!, "teamName", this.teamName)
    }

    // field amount -> how many fields the entity should move (is decided with rolling the dice)
    fun move(board: GameBoard, fieldAmount: Int, fieldHeight: Double) {
        if (this.livingEntity == null) return

        if (this.currentFieldId != null) {
            val oldField: GameField = board.getField(this.currentFieldId!!) ?: return
            oldField.isTaken = false
        }

        val newFieldId: Int = if (this.currentFieldId == null) fieldAmount else this.currentFieldId!! + fieldAmount
        this.currentFieldId = newFieldId

        val newField: GameField = board.getField(this.currentFieldId!!) ?: return
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