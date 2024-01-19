package net.spacetivity.ludo.arena

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.extensions.clearPhaseItems
import net.spacetivity.ludo.phase.GamePhase
import net.spacetivity.ludo.team.GameTeam
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import java.util.*

class GameArena(
    val id: String,
    val gameWorld: World,
    val viewPlatformLocation: Location,
    var status: GameArenaStatus,
    var phase: GamePhase
) {

    val maxPlayers: Int = 4
    val currentPlayers: MutableSet<UUID> = mutableSetOf()

    var arenaHost: Player? = null

    init {
        if (this.status == GameArenaStatus.READY) this.phase.start()
    }

    fun sendArenaMessage(message: Component) {
        for (player: Player? in this.currentPlayers.map { Bukkit.getPlayer(it) }) {
            if (player == null) continue
            player.sendMessage(message)
        }
    }

    fun join(player: Player) {
        if (this.currentPlayers.contains(player.uniqueId)) {
            player.sendMessage(Component.text("Already in arena!"))
            return
        }

        if (!this.phase.isIdle()) {
            player.sendMessage("The game is already running!")
            return
        }

        if (this.currentPlayers.size >= this.maxPlayers) {
            player.sendMessage(Component.text("Arena is full!"))
            return
        }

        val gameTeam: GameTeam? = LudoGame.instance.gameTeamHandler.gameTeams.get(this.id).filter { it.teamMembers.isEmpty() }.randomOrNull()

        if (gameTeam == null) {
            println(LudoGame.instance.gameTeamHandler.gameTeams.size()) //TODO: REMOVE THAT
            player.sendMessage(Component.text("No empty team was found for you... Join cancelled!"))
            return
        }

        player.sendMessage(Component.text("You joined the arena!", NamedTextColor.GREEN))

        if (this.currentPlayers.isEmpty() || this.arenaHost == null) {
            this.arenaHost = player
            player.sendMessage(Component.text("You are now the arena host!", NamedTextColor.GREEN))
        }

        this.currentPlayers.add(player.uniqueId)
        this.phase.setupPlayerInventory(player)

        gameTeam.join(player)
    }

    fun quit(player: Player) {
        if (!this.currentPlayers.contains(player.uniqueId)) {
            player.sendMessage(Component.text("Not in arena!"))
            return
        }

        player.sendMessage("You left the arena!")
        player.clearPhaseItems()

        LudoGame.instance.gameTeamHandler.getTeamOfPlayer(this.id, player.uniqueId)?.quit(player)
        this.currentPlayers.remove(player.uniqueId)

        if (this.phase.isIngame() && this.currentPlayers.isEmpty()) {
            reset()
            return
        }

        if (this.arenaHost != null && this.arenaHost!!.uniqueId == player.uniqueId) {
            this.arenaHost = null
            this.arenaHost = findNewHost()

            if (this.arenaHost == null) {
                println("No new host for arena $id could be determined!")
                reset()
            } else {
                this.arenaHost?.sendMessage(Component.text("You are now the new Game-Host!", NamedTextColor.YELLOW))
            }

        }
    }

    fun reset() {
        this.currentPlayers.clear()

        for (gameTeam: GameTeam in LudoGame.instance.gameTeamHandler.gameTeams.values()) {
            for (teamMember: UUID in gameTeam.teamMembers) {
                val player = Bukkit.getPlayer(teamMember) ?: continue
                gameTeam.quit(player)
            }
        }

        LudoGame.instance.gameEntityHandler.clearEntitiesFromArena(this.id)
        if (!this.phase.isIdle()) LudoGame.instance.gamePhaseHandler.initIndexPhase(this)
    }

    private fun findNewHost(): Player? {
        if (this.currentPlayers.isEmpty() || this.currentPlayers.size == 1) return null

        val newHostUuid: UUID = if (this.arenaHost == null)
            this.currentPlayers.random()
        else
            this.currentPlayers.filter { it != this.arenaHost?.uniqueId }.random()

        return Bukkit.getPlayer(newHostUuid)
    }

}