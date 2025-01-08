package net.spacetivity.blocko.team

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.extensions.translateMessage
import net.spacetivity.blocko.player.GamePlayer
import java.util.*

class GameTeam(val name: String, val color: NamedTextColor, val teamId: Int) {

    val teamMembers: MutableSet<UUID> = mutableSetOf()
    val teamLocations: MutableSet<GameTeamLocation> = mutableSetOf()

    var deactivated: Boolean = false

    fun join(gamePlayer: GamePlayer) {
        if (isFull()) {
            gamePlayer.translateMessage("blocko.team.already_full")
            return
        }

        if (containsTeam(gamePlayer.uuid)) {
            gamePlayer.translateMessage("blocko.team.yourself_already_in_team")
            return
        }

        this.teamMembers.add(gamePlayer.uuid)
        gamePlayer.teamName = this.name

        gamePlayer.translateMessage("blocko.team.join",
            Placeholder.parsed("team_color", "<${this.color.asHexString()}>"),
            Placeholder.parsed("team_name", this.name.lowercase().replaceFirstChar { it.uppercase() }))

        BlockoGame.instance.playerFormatHandler.setTablistFormatForAll()
    }

    fun quit(gamePlayer: GamePlayer) {
        if (!containsTeam(gamePlayer.uuid)) {
            gamePlayer.translateMessage("blocko.team.not_in_team")
            return
        }

        this.teamMembers.remove(gamePlayer.uuid)
        gamePlayer.teamName = null
        gamePlayer.translateMessage("blocko.team.quit")

        BlockoGame.instance.playerFormatHandler.setTablistFormatForAll()
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
