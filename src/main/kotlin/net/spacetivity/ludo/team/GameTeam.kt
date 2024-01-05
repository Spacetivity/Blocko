package net.spacetivity.ludo.team

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.spacetivity.ludo.utils.MetadataUtils
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.*

class GameTeam(val name: String, val color: TextColor, val headValue: String) {

    val teamMembers: MutableSet<UUID> = mutableSetOf()
    val gameEntities: MutableMap<UUID, Int> = mutableMapOf()
    var spawnLocation: Location? = null

    fun join(player: Player) {
        if (isFull()) {
            player.sendMessage(Component.text("This team is already full!"))
            return
        }

        if (containsTeam(player.uniqueId)) {
            player.sendMessage(Component.text("You are already in this team!"))
            return
        }

        this.teamMembers.add(player.uniqueId)
        MetadataUtils.set(player, "teamName", this.name)
        player.sendMessage(Component.text("You are now in team: ${this.name}"))
    }

    fun quit(player: Player) {
        if (!containsTeam(player.uniqueId)) {
            player.sendMessage(Component.text("You are not in this team!"))
            return
        }

        this.teamMembers.remove(player.uniqueId)
        MetadataUtils.remove(player, "teamName")
        player.sendMessage(Component.text("You are left your team."))
    }

    private fun isFull(): Boolean = this.teamMembers.isNotEmpty()
    private fun containsTeam(uuid: UUID): Boolean = this.teamMembers.contains(uuid)

}
