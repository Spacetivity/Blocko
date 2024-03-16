package net.spacetivity.ludo.arena

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.dice.DiceHandler
import net.spacetivity.ludo.extensions.*
import net.spacetivity.ludo.phase.GamePhase
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.scoreboard.GameScoreboardUtils
import net.spacetivity.ludo.stats.StatsPlayer
import net.spacetivity.ludo.team.GameTeam
import net.spacetivity.ludo.team.GameTeamOptions
import net.spacetivity.ludo.translation.Translation
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.Player
import java.util.*
import java.util.function.Predicate

class GameArena(
    val id: String,
    val gameWorld: World,
    var status: GameArenaStatus,
    var phase: GamePhase,
) {

    var locked: Boolean = false
    var waitForActualPlayers: Boolean = true
    var teamOptions: GameTeamOptions = GameTeamOptions.TWO_BY_ONE

    val currentPlayers: MutableSet<GamePlayer> = mutableSetOf()
    var arenaHost: GamePlayer? = null

    val invitedPlayers: MutableSet<UUID> = mutableSetOf()

    init {
        if (this.status == GameArenaStatus.READY) this.phase.start()
    }

    @Deprecated(level = DeprecationLevel.WARNING, message = "Will be replaced")
    fun sendArenaMessage(message: Component) {
        for (player: Player? in this.currentPlayers.filter { !it.isAI }.map { it.toBukkitInstance() }) {
            if (player == null) continue
            player.sendMessage(message)
        }
    }

    fun sendArenaMessage(key: String, vararg toReplace: TagResolver) {
        val translation: Translation = LudoGame.instance.translationHandler.getSelectedTranslation()
        for (gamePlayer: GamePlayer in this.currentPlayers.filter { !it.isAI }) {
            gamePlayer.sendMessage(translation.validateLine(key, *toReplace))
        }
    }

    fun sendArenaSound(sound: Sound, volume: Float) {
        for (player: Player? in this.currentPlayers.filter { !it.isAI }.map { it.toBukkitInstance() }) {
            if (player == null) continue
            player.playSound(player.location, sound, volume, 1F)
        }
    }

    fun sendArenaInvite(sender: GamePlayer, receiverName: String) {
        val gameArena: GameArena = LudoGame.instance.gameArenaHandler.getArena(sender.arenaId) ?: return

        if (!gameArena.phase.isIdle()) {
            sender.sendMessage(Component.text("Your game has already started!", NamedTextColor.RED))
            return
        }

        val senderBukkitPlayer: Player = sender.toBukkitInstance() ?: return
        val receiverBukkitPlayer: Player? = Bukkit.getPlayer(receiverName)

        if (receiverBukkitPlayer == null) {
            sender.sendMessage(Component.text("This player does not exist!", NamedTextColor.RED))
            return
        }

        val receiverGamePlayer: GamePlayer? = receiverBukkitPlayer.toGamePlayerInstance()
        if (receiverGamePlayer != null && receiverGamePlayer.arenaId == sender.arenaId) {
            sender.sendMessage(Component.text("This player is already in your arena!", NamedTextColor.RED))
            return
        }

        if (senderBukkitPlayer.name.equals(receiverName, true)) {
            sender.sendMessage(Component.text("You cannot invite yourself!", NamedTextColor.RED))
            return
        }

        if (this.invitedPlayers.contains(receiverBukkitPlayer.uniqueId)) {
            sender.sendMessage(Component.text("This player was already invited!", NamedTextColor.RED))
            return
        }

        if (gameArena.currentPlayers.size >= gameArena.teamOptions.playerCount) {
            sender.sendMessage(Component.text("Arena is already full!", NamedTextColor.RED))
            return
        }

        this.invitedPlayers.add(receiverBukkitPlayer.uniqueId)

        senderBukkitPlayer.translateMessage("blocko.arena.invite_sent", Placeholder.parsed("name", receiverName))
        receiverBukkitPlayer.translateMessage("blocko.arena.invite_received", Placeholder.parsed("name", senderBukkitPlayer.name), Placeholder.parsed("id", sender.arenaId))

        LudoGame.instance.gameArenaSignHandler.updateArenaSign(this)
    }

    fun join(uuid: UUID, isAI: Boolean): Boolean {
        val gamePlayer = GamePlayer(uuid, this.id, null, isAI)

        if (this.currentPlayers.any { it.uuid == gamePlayer.uuid }) {
            gamePlayer.sendMessage(Component.text("Already in arena!"))
            return false
        }

        if (!this.phase.isIdle()) {
            gamePlayer.sendMessage(Component.text("The game is already running!", NamedTextColor.DARK_RED))
            return false
        }

        if (this.currentPlayers.size >= this.teamOptions.playerCount) {
            gamePlayer.sendMessage(Component.text("Arena is full!", NamedTextColor.DARK_RED))
            return false
        }

        if (!isAI && this.locked && !this.invitedPlayers.contains(uuid)) {
            gamePlayer.sendMessage(Component.text("This arena can only be entered by invitation of the host."))
            return false
        }

        gamePlayer.sendMessage(Component.text("You joined the arena!", NamedTextColor.GREEN))

        if (!gamePlayer.isAI && (this.currentPlayers.isEmpty() || this.arenaHost == null)) {
            this.arenaHost = gamePlayer
            gamePlayer.sendMessage(Component.text("You are now the arena host!", NamedTextColor.GREEN))
        }

        this.currentPlayers.add(gamePlayer)

        if (!isAI) {
            this.phase.setupPlayerInventory(gamePlayer.toBukkitInstance()!!)

            val neededPlayerCount: Int = if (this.waitForActualPlayers) this.teamOptions.playerCount else 1
            this.phase.countdown?.tryStartup(Predicate { playerCount -> playerCount == neededPlayerCount })

            GameScoreboardUtils.setGameSidebar(gamePlayer)
        }

        LudoGame.instance.gameArenaSignHandler.updateArenaSign(this)
        return true
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

        if (LudoGame.instance.diceHandler.dicingPlayers.containsKey(gamePlayer.uuid))
            LudoGame.instance.diceHandler.dicingPlayers.remove(gamePlayer.uuid)

        if (!gamePlayer.isAI) {
            LudoGame.instance.statsPlayerHandler.getStatsPlayer(player.uniqueId)?.updateDbEntry()
            GameScoreboardUtils.removeGameSidebar(player)
        }

        if (gamePlayer.teamName != null)
            LudoGame.instance.gameTeamHandler.getTeamOfPlayer(this.id, player.uniqueId)?.quit(gamePlayer)

        this.invitedPlayers.removeIf { it == player.uniqueId }
        this.currentPlayers.removeIf { it.uuid == player.uniqueId }

        if (this.currentPlayers.isEmpty()) this.phase.countdown?.cancel()

        if (this.phase.isIngame() && (this.currentPlayers.isEmpty() || this.currentPlayers.size == 1)) {
            reset(false)
            return
        }

        if (this.phase.isIdle() && this.phase.countdown != null && this.phase.countdown!!.isRunning) {
            this.phase.countdown!!.cancel()
            sendArenaMessage(Component.text("Countdown stopped! To less players...", NamedTextColor.YELLOW))
        }

        if (this.arenaHost != null && this.arenaHost!!.uuid == player.uniqueId) {
            this.arenaHost = null
            this.arenaHost = findNewHost()

            if (this.arenaHost == null) {
                reset(false)
            } else {
                this.arenaHost?.sendMessage(Component.text("You are now the new Game-Host!", NamedTextColor.YELLOW))
            }
        }

        LudoGame.instance.gameArenaSignHandler.updateArenaSign(this)
    }

    fun reset(shutdown: Boolean) {
        this.phase.countdown?.cancel()

        for (player: Player? in this.currentPlayers.filter { !it.isAI }.map { it.toBukkitInstance() }) {
            if (player == null) continue

            GameScoreboardUtils.removeGameSidebar(player)
            LudoGame.instance.bossbarHandler.clearBossbars(player)

            this.phase.clearPlayerInventory(player)
        }

        for (gamePlayer: GamePlayer in this.currentPlayers) {
            val statsPlayer: StatsPlayer? = LudoGame.instance.statsPlayerHandler.getStatsPlayer(gamePlayer.uuid)
            if (!gamePlayer.isAI && statsPlayer != null && !shutdown) statsPlayer.updateDbEntry()

            for (gameTeam: GameTeam in LudoGame.instance.gameTeamHandler.gameTeams[this.id]) {
                gameTeam.quit(gamePlayer)
            }
        }

        val diceHandler: DiceHandler = LudoGame.instance.diceHandler

        for (currentPlayer: GamePlayer in this.currentPlayers) {
            if (!diceHandler.dicingPlayers.containsKey(currentPlayer.uuid)) continue
            diceHandler.dicingPlayers.remove(currentPlayer.uuid)
        }

        this.invitedPlayers.clear()
        this.currentPlayers.clear()

        this.locked = false
        this.waitForActualPlayers = true
        this.teamOptions = GameTeamOptions.TWO_BY_ONE
        this.arenaHost = null

        LudoGame.instance.gameEntityHandler.clearEntitiesFromArena(this.id)

        if (!this.phase.isIdle()) LudoGame.instance.gamePhaseHandler.initIndexPhase(this)
        LudoGame.instance.gameArenaSignHandler.updateArenaSign(this)
    }

    fun isGameOver(): Boolean {
        val finishedGamePlayers: List<GamePlayer> = this.currentPlayers.filter { it.getTeam().deactivated }.toList()
        val enoughGamePlayersFinished: Boolean = finishedGamePlayers.size == (this.teamOptions.playerCount - 1)
        return enoughGamePlayersFinished
    }

    private fun findNewHost(): GamePlayer? {
        val actualCurrentPlayers: List<GamePlayer> = this.currentPlayers.filter { !it.isAI }

        if (actualCurrentPlayers.isEmpty()) return null

        val newHostPlayer: GamePlayer = if (this.arenaHost == null)
            actualCurrentPlayers.random()
        else
            actualCurrentPlayers.filter { it.uuid != this.arenaHost?.uuid }.random()

        return newHostPlayer
    }

}