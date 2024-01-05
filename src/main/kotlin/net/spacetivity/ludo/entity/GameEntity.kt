package net.spacetivity.ludo.entity

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.board.GameBoard
import net.spacetivity.ludo.board.GameField
import net.spacetivity.ludo.team.GameTeam
import net.spacetivity.ludo.utils.ItemUtils
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType

data class GameEntity(val teamName: String) {

    var currentFieldId: Int? = null
    var livingEntity: ArmorStand? = null

    fun spawn(location: Location) {
        if (this.livingEntity != null) return
        val gameTeam: GameTeam = LudoGame.instance.gameTeamHandler.getTeam(this.teamName) ?: return

        this.livingEntity = location.world.spawnEntity(location, EntityType.ARMOR_STAND) as ArmorStand
        this.livingEntity!!.equipment.helmet = ItemUtils(Material.PLAYER_HEAD).setOwner(gameTeam.headValue).build()
        this.livingEntity!!.isSilent = true;
        this.livingEntity!!.isGlowing = true
        this.livingEntity!!.isSmall = true
        this.livingEntity!!.isInvisible = true
        this.livingEntity!!.setGravity(false)
        this.livingEntity!!.setBasePlate(false)
        this.livingEntity!!.setAI(false)
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
        this.livingEntity!!.teleport(newField.getWorldPosition(fieldHeight))
    }

    fun despawn() {
        if (this.livingEntity == null) return
        this.livingEntity!!.remove()
        this.livingEntity = null
    }

}