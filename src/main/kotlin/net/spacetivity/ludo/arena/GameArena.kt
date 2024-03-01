package net.spacetivity.ludo.arena

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.dice.DiceHandler
import net.spacetivity.ludo.extensions.*
import net.spacetivity.ludo.phase.GamePhase
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.team.GameTeam
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.Player
import java.util.*

class GameArena(
    val id: String,
    val gameWorld: World,
    var status: GameArenaStatus,
    var phase: GamePhase
) {

    val maxPlayers: Int = 4
    val currentPlayers: MutableSet<GamePlayer> = mutableSetOf()
    var arenaHost: GamePlayer? = null

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

    fun join(uuid: UUID, gameTeam: GameTeam, isAI: Boolean) {
        val gamePlayer = GamePlayer(uuid, this.id, gameTeam.name, isAI)

        if (this.currentPlayers.any { it.uuid == gamePlayer.uuid }) {
            gamePlayer.sendMessage(Component.text("Already in arena!"))
            return
        }

        if (!this.phase.isIdle()) {
            gamePlayer.sendMessage(Component.text("The game is already running!", NamedTextColor.DARK_RED))
            return
        }

        if (this.currentPlayers.size >= this.maxPlayers) {
            gamePlayer.sendMessage(Component.text("Arena is full!", NamedTextColor.DARK_RED))
            return
        }

        gamePlayer.setGameMode(GameMode.ADVENTURE)
        gamePlayer.setFlying(true)
        gamePlayer.sendMessage(Component.text("You joined the arena!", NamedTextColor.GREEN))

        if (!gamePlayer.isAI && (this.currentPlayers.isEmpty() || this.arenaHost == null)) {
            this.arenaHost = gamePlayer
            gamePlayer.sendMessage(Component.text("You are now the arena host!", NamedTextColor.GREEN))
        }

        this.currentPlayers.add(gamePlayer)

        if (!isAI) {
            this.phase.setupPlayerInventory(gamePlayer.toBukkitInstance()!!)
            this.phase.countdown?.tryStartup()
        }

        gameTeam.join(gamePlayer)
    }

    fun quit(player: Player) {
        if (this.currentPlayers.none { it.uuid == player.uniqueId }) {
            player.sendMessage(Component.text("Not in arena!"))
            return
        }

        player.sendMessage(Component.text("You left the arena!", NamedTextColor.YELLOW))
        player.clearPhaseItems()

        LudoGame.instance.bossbarHandler.clearBossbars(player)

        val gamePlayer: GamePlayer = this.currentPlayers.find { it.uuid == player.uniqueId } ?: return
        gamePlayer.setFlying(false)

        if (LudoGame.instance.diceHandler.dicingPlayers.containsKey(gamePlayer.uuid))
            LudoGame.instance.diceHandler.dicingPlayers.remove(gamePlayer.uuid)

        LudoGame.instance.gameTeamHandler.getTeamOfPlayer(this.id, player.uniqueId)?.quit(gamePlayer)
        this.currentPlayers.removeIf { it.uuid == player.uniqueId }

        if (this.currentPlayers.isEmpty()) this.phase.countdown?.cancel()

        if (this.phase.isIngame() && this.currentPlayers.isEmpty()) {
            reset()
            return
        }

        if (this.arenaHost != null && this.arenaHost!!.uuid == player.uniqueId) {
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

        for (gameTeam: GameTeam in LudoGame.instance.gameTeamHandler.gameTeams[this.id]) {
            val teamMembers = gameTeam.teamMembers.toMutableList()
            for (teamMemberUuid: UUID in teamMembers) {
                val gamePlayer: GamePlayer = this.currentPlayers.find { it.uuid == teamMemberUuid } ?: continue
                gameTeam.quit(gamePlayer)
            }
        }

        val diceHandler: DiceHandler = LudoGame.instance.diceHandler

        for (currentPlayer in this.currentPlayers) {
            if (!diceHandler.dicingPlayers.containsKey(currentPlayer.uuid)) continue
            diceHandler.dicingPlayers.remove(currentPlayer.uuid)
        }

        this.currentPlayers.clear()

        LudoGame.instance.gameEntityHandler.clearEntitiesFromArena(this.id)
        if (!this.phase.isIdle()) LudoGame.instance.gamePhaseHandler.initIndexPhase(this)

        println("Arena ${this.id} was reset! Phase is now: ${this.phase.name}")
    }

    fun isGameOver(): Boolean {
        val finishedGamePlayers: List<GamePlayer> = this.currentPlayers.filter { it.getTeam().deactivated }.toList()
        val enoughGamePlayersFinished: Boolean = finishedGamePlayers.size == (this.maxPlayers - 1)
        return enoughGamePlayersFinished
    }

    private fun findNewHost(): GamePlayer? {
        val actualCurrentPlayers: List<GamePlayer> = this.currentPlayers.filter { !it.isAI }

        if (actualCurrentPlayers.isEmpty() || actualCurrentPlayers.size == 1) return null

        val newHostPlayer: GamePlayer = if (this.arenaHost == null)
            actualCurrentPlayers.random()
        else
            actualCurrentPlayers.filter { it.uuid != this.arenaHost?.uuid }.random()

        return newHostPlayer
    }

}