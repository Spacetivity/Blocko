package net.spacetivity.ludo.team

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.ludo.extensions.sendMessage
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.utils.ScoreboardUtils
import org.bukkit.scoreboard.Team
import java.util.*

class GameTeam(val name: String, val color: NamedTextColor, val teamId: Int) {

    val scoreboardTeam: Team = ScoreboardUtils.registerScoreboardTeam( "team_${this.name}", this.color)
    val teamMembers: MutableSet<UUID> = mutableSetOf() //TODO: Change this to a GamePlayer instance, so that AI can be a team holder
    val teamLocations: MutableSet<GameTeamLocation> = mutableSetOf()

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
        gamePlayer.sendMessage(Component.text("You are now in team: ${this.name}", NamedTextColor.GREEN))
    }

    fun quit(gamePlayer: GamePlayer) {
        if (!containsTeam(gamePlayer.uuid)) {
            gamePlayer.sendMessage(Component.text("You are not in this team!"))
            return
        }

        this.teamMembers.remove(gamePlayer.uuid)
        gamePlayer.sendMessage(Component.text("You are left your team.", NamedTextColor.YELLOW))
    }

    fun getFreeSpawnLocation(): GameTeamLocation? {
        return this.teamLocations.firstOrNull { !it.isTaken }
    }

    private fun isFull(): Boolean = this.teamMembers.isNotEmpty()
    private fun containsTeam(uuid: UUID): Boolean = this.teamMembers.contains(uuid)

}
