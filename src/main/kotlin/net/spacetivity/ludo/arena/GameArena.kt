package net.spacetivity.ludo.arena

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.ludo.LudoGame
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import java.util.*

class GameArena(val id: String, val gameWorld: World, val viewPlatformLocation: Location, var status: GameArenaStatus) {

    val maxPlayers: Int = 4

    val currentPlayers: MutableSet<UUID> = mutableSetOf()
    var arenaHost: Player? = null

    fun joinArena(player: Player) {
        if (this.currentPlayers.contains(player.uniqueId)) {
            player.sendMessage(Component.text("Already in arena!"))
            return
        }

        if (this.currentPlayers.size >= this.maxPlayers) {
            player.sendMessage(Component.text("Arena is full!"))
            return
        }

        if (this.currentPlayers.isEmpty()) {
            this.arenaHost = player
            player.sendMessage(Component.text("You are now the arena host!", NamedTextColor.YELLOW))
        }

        this.currentPlayers.add(player.uniqueId)
        player.sendMessage("You joined the arena!")
    }

    fun quitArena(player: Player) {
        if (!this.currentPlayers.contains(player.uniqueId)) {
            player.sendMessage(Component.text("Not in arena!"))
            return
        }


        if (this.currentPlayers.size.dec() == 0) {
            resetArena()
            return
        }

        if (this.arenaHost != null && player.uniqueId == this.arenaHost!!.uniqueId) {
            this.arenaHost = null
            this.arenaHost = findNewHost()

            if (this.arenaHost == null) {
                println("No new host for arena $id could be determined!")
                resetArena()
                return
            }

            this.arenaHost?.sendMessage(Component.text("You are now the new Game-Host!", NamedTextColor.YELLOW))
        }

        this.currentPlayers.remove(player.uniqueId)
        player.sendMessage("You left the arena!")
    }

    fun startGame() {

    }

    fun resetArena() {
        this.currentPlayers.clear()
        LudoGame.instance.gameTeamHandler.gameTeams.clear()
        LudoGame.instance.gameEntityHandler.clearEntitiesFromArena(this.id)
    }

    private fun findNewHost(): Player? {
        val newHostUuid: UUID = this.currentPlayers.filter { it != this.arenaHost!!.uniqueId }.random()
        return Bukkit.getPlayer(newHostUuid) ?: findNewHost()
    }

}