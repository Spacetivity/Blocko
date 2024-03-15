package net.spacetivity.ludo.team

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.ludo.extensions.sendMessage
import net.spacetivity.ludo.player.GamePlayer
import java.util.*

class GameTeam(val name: String, val color: NamedTextColor, val teamId: Int) {

    val teamMembers: MutableSet<UUID> = mutableSetOf()
    val teamLocations: MutableSet<GameTeamLocation> = mutableSetOf()

    var deactivated: Boolean = false

    fun join(gamePlayer: GamePlayer) {
        if (isFull()) {
            gamePlayer.sendMessage(Component.text("This team is already full!"))
            return
        }

        if (containsTeam(gamePlayer.uuid)) {
            gamePlayer.sendMessage(Component.text("You are already in this team!"))
            return
        }

        this.teamMembers.add(gamePlayer.uuid)
        gamePlayer.teamName = this.name
        gamePlayer.sendMessage(Component.text("You are now in team: ${this.name}", NamedTextColor.GREEN))
    }

    fun quit(gamePlayer: GamePlayer) {
        if (!containsTeam(gamePlayer.uuid)) {
            gamePlayer.sendMessage(Component.text("You are not in this team!"))
            return
        }

        this.teamMembers.remove(gamePlayer.uuid)
        gamePlayer.teamName = null
        gamePlayer.sendMessage(Component.text("You are left your team.", NamedTextColor.YELLOW))
    }

    fun getFreeSpawnLocation(): GameTeamLocation? {
        val freeSpawns: MutableList<GameTeamLocation> = mutableListOf()

        for (teamLocation: GameTeamLocation in this.teamLocations) {
            if (teamLocation.isTaken) continue
            freeSpawns.add(teamLocation)
        }

        return freeSpawns.randomOrNull()
    }

    private fun isFull(): Boolean = this.teamMembers.isNotEmpty()
    private fun containsTeam(uuid: UUID): Boolean = this.teamMembers.contains(uuid)

}
