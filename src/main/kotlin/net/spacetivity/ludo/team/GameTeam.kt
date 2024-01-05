package net.spacetivity.ludo.team

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.ludo.utils.MetadataUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team
import java.util.*

class GameTeam(val name: String, val color: NamedTextColor, val headValue: String) {

    var scoreboardTeam: Team?

    val teamMembers: MutableSet<UUID> = mutableSetOf()
    val gameEntities: MutableMap<UUID, Int> = mutableMapOf()
    var spawnLocation: Location? = null

    init {
        val mainScoreboard = Bukkit.getScoreboardManager().mainScoreboard
        val sbTeamName = "sh_team_${this.name}"

        this.scoreboardTeam = mainScoreboard.getTeam(sbTeamName)
        if (this.scoreboardTeam == null) this.scoreboardTeam = mainScoreboard.registerNewTeam(sbTeamName)

        this.scoreboardTeam?.color(this.color)
        this.scoreboardTeam?.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER)
    }

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
