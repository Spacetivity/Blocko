package net.spacetivity.ludo.arena

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.extensions.clearPhaseItems
import net.spacetivity.ludo.phase.GamePhase
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.team.GameTeam
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound
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
    val currentPlayers: MutableSet<GamePlayer> = mutableSetOf()

    var arenaHost: Player? = null

    init {
        if (this.status == GameArenaStatus.READY) this.phase.start()
    }

    fun sendArenaMessage(message: Component) {
        for (player: Player? in this.currentPlayers.filter { !it.isAI }.map { it.toBukkitInstance() }) {
            if (player == null) continue
            player.sendMessage(message)
        }
    }

    fun sendArenaSound(sound: Sound) {
        for (player: Player? in this.currentPlayers.filter { !it.isAI }.map { it.toBukkitInstance() }) {
            if (player == null) continue
            player.playSound(player.location, sound, 0.2F, 1F)
        }
    }

    fun join(player: Player) {
        if (this.currentPlayers.any { it.uuid == player.uniqueId }) {
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
            player.sendMessage(Component.text("No empty team was found for you... Join cancelled!"))
            return
        }

        player.sendMessage(Component.text("You joined the arena!", NamedTextColor.GREEN))

        if (this.currentPlayers.isEmpty() || this.arenaHost == null) {
            this.arenaHost = player
            player.sendMessage(Component.text("You are now the arena host!", NamedTextColor.GREEN))
        }

        this.currentPlayers.add(GamePlayer(player.uniqueId, this.id, gameTeam.name, false, null))
        this.phase.setupPlayerInventory(player)
        this.phase.countdown?.tryStartup()

        gameTeam.join(player)
    }

    fun quit(player: Player) {
        if (this.currentPlayers.none { it.uuid == player.uniqueId }) {
            player.sendMessage(Component.text("Not in arena!"))
            return
        }

        player.sendMessage(Component.text("You left the arena!", NamedTextColor.YELLOW))
        player.clearPhaseItems()

        LudoGame.instance.gameTeamHandler.getTeamOfPlayer(this.id, player.uniqueId)?.quit(player)
        this.currentPlayers.removeIf { it.uuid == player.uniqueId }

        if (this.currentPlayers.isEmpty()) this.phase.countdown?.cancel()

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
        this.phase.countdown?.cancel()

        for (player: Player? in this.currentPlayers.filter { !it.isAI }.map { it.toBukkitInstance() }) {
            if (player == null) continue
            this.phase.clearPlayerInventory(player)
        }

        this.currentPlayers.clear()

        for (gameTeam: GameTeam in LudoGame.instance.gameTeamHandler.gameTeams.values()) {
            for (teamMember: UUID in gameTeam.teamMembers) {
                val player = Bukkit.getPlayer(teamMember) ?: continue
                gameTeam.quit(player)
            }
        }

        LudoGame.instance.gameEntityHandler.clearEntitiesFromArena(this.id)
        if (!this.phase.isIdle()) LudoGame.instance.gamePhaseHandler.initIndexPhase(this)

        println("Arena ${this.id} was reset! Phase is now: ${this.phase.name}")
    }

    private fun findNewHost(): Player? {
        val actualCurrentPlayers: List<GamePlayer> = this.currentPlayers.filter { !it.isAI }

        if (actualCurrentPlayers.isEmpty() || actualCurrentPlayers.size == 1) return null

        val newHostUuid: Player? = if (this.arenaHost == null)
            actualCurrentPlayers.random().toBukkitInstance()
        else
            actualCurrentPlayers.filter { it.uuid != this.arenaHost?.uniqueId }.random().toBukkitInstance()

        return newHostUuid
    }

}