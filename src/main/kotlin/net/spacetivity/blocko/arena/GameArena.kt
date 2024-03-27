package net.spacetivity.blocko.arena

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.dice.DiceHandler
import net.spacetivity.blocko.extensions.*
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.lobby.LobbySpawn
import net.spacetivity.blocko.phase.GamePhase
import net.spacetivity.blocko.phase.GamePhaseMode
import net.spacetivity.blocko.phase.impl.IngamePhase
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.scoreboard.GameScoreboardUtils
import net.spacetivity.blocko.stats.StatsPlayer
import net.spacetivity.blocko.team.GameTeam
import net.spacetivity.blocko.team.GameTeamOptions
import org.bukkit.Bukkit
import org.bukkit.Location
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
    val yLevel: Double,
    val location: Location,
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

    fun sendArenaMessage(key: String, vararg toReplace: TagResolver) {
        this.currentPlayers.filter { !it.isAI }.forEach { it.translateMessage(key, *toReplace) }
    }

    fun sendArenaSound(sound: Sound, volume: Float) {
        for (player: Player? in this.currentPlayers.filter { !it.isAI }.map { it.toBukkitInstance() }) {
            if (player == null) continue
            player.playSound(player.location, sound, volume, 1F)
        }
    }

    fun sendArenaInvite(sender: GamePlayer, receiverName: String) {
        val gameArena: GameArena = BlockoGame.instance.gameArenaHandler.getArena(sender.arenaId) ?: return

        if (!gameArena.phase.isIdle()) {
            sender.translateMessage("blocko.arena.game_already_started")
            return
        }

        val senderBukkitPlayer: Player = sender.toBukkitInstance() ?: return
        val receiverBukkitPlayer: Player? = Bukkit.getPlayer(receiverName)

        if (receiverBukkitPlayer == null) {
            sender.translateMessage("blocko.utils.player_not_found")
            return
        }

        val receiverGamePlayer: GamePlayer? = receiverBukkitPlayer.toGamePlayerInstance()
        if (receiverGamePlayer != null && receiverGamePlayer.arenaId == sender.arenaId) {
            sender.translateMessage("blocko.arena.player_already_in_arena")
            return
        }

        if (senderBukkitPlayer.name.equals(receiverName, true)) {
            sender.translateMessage("blocko.arena.invite_yourself")
            return
        }

        if (this.invitedPlayers.contains(receiverBukkitPlayer.uniqueId)) {
            sender.translateMessage("blocko.arena.player_already_invited")
            return
        }

        if (gameArena.currentPlayers.size >= gameArena.teamOptions.playerCount) {
            sender.translateMessage("blocko.arena.already_full")
            return
        }

        this.invitedPlayers.add(receiverBukkitPlayer.uniqueId)

        senderBukkitPlayer.translateMessage("blocko.arena.invite_sent", Placeholder.parsed("name", receiverName))
        receiverBukkitPlayer.translateMessage("blocko.arena.invite_received", Placeholder.parsed("name", senderBukkitPlayer.name), Placeholder.parsed("id", sender.arenaId))

        BlockoGame.instance.gameArenaSignHandler.updateArenaSign(this)
    }

    fun join(uuid: UUID, isAI: Boolean): Boolean {
        val bukkitPlayer: Player? = Bukkit.getPlayer(uuid)
        val name: String = bukkitPlayer?.name ?: BlockoGame.instance.botNamesFile.botNames.random()

        val gamePlayer = GamePlayer(uuid, name, this.id, null, isAI)

        if (this.currentPlayers.any { it.uuid == gamePlayer.uuid }) {
            gamePlayer.translateMessage("blocko.arena.yourself_already_in_arena")
            return false
        }

        if (!this.phase.isIdle()) {
            gamePlayer.translateMessage("blocko.arena.game_already_started")
            return false
        }

        if (this.currentPlayers.size >= this.teamOptions.playerCount) {
            gamePlayer.translateMessage("blocko.arena.already_full")
            return false
        }

        if (!isAI && this.locked && !this.invitedPlayers.contains(uuid)) {
            gamePlayer.translateMessage("blocko.arena.not_invited_by_host")
            return false
        }

        sendArenaMessage("blocko.arena.join", Placeholder.parsed("name", gamePlayer.name))

        if (!gamePlayer.isAI && (this.currentPlayers.isEmpty() || this.arenaHost == null)) {
            this.arenaHost = gamePlayer
            gamePlayer.translateMessage("blocko.arena.host_join")
        }

        this.currentPlayers.add(gamePlayer)

        if (!isAI) {
            this.phase.setupPlayerInventory(gamePlayer.toBukkitInstance()!!)

            val neededPlayerCount: Int = if (this.waitForActualPlayers) this.teamOptions.playerCount else 1
            this.phase.countdown?.tryStartup(Predicate { playerCount -> playerCount == neededPlayerCount })

            togglePlayerVisibility(bukkitPlayer!!, true)

            GameScoreboardUtils.setGameSidebar(gamePlayer)
            BlockoGame.instance.playerFormatHandler.setTablistFormatForAll()
        } else {
            val aiStatsPlayer = StatsPlayer(uuid, 0, 0, 0, 0, 0)
            BlockoGame.instance.statsPlayerHandler.cachedStatsPlayers.add(aiStatsPlayer)
        }

        BlockoGame.instance.gameArenaSignHandler.updateArenaSign(this)
        return true
    }

    fun quit(player: Player) {
        if (this.currentPlayers.none { it.uuid == player.uniqueId }) return

        sendArenaMessage("blocko.arena.quit", Placeholder.parsed("name", player.name))
        player.clearPhaseItems()

        val lobbySpawn: LobbySpawn? = BlockoGame.instance.lobbySpawnHandler.lobbySpawn
        if (lobbySpawn != null && player.world.name != lobbySpawn.worldName) player.teleportAsync(lobbySpawn.toBukkitInstance()).thenAccept {
            togglePlayerVisibility(player, false)
        }

        BlockoGame.instance.bossbarHandler.clearBossbars(player)

        val gamePlayer: GamePlayer = this.currentPlayers.find { it.uuid == player.uniqueId } ?: return

        if (BlockoGame.instance.diceHandler.dicingPlayers.containsKey(gamePlayer.uuid))
            BlockoGame.instance.diceHandler.dicingPlayers.remove(gamePlayer.uuid)

        if (!gamePlayer.isAI) {
            BlockoGame.instance.statsPlayerHandler.getStatsPlayer(player.uniqueId)?.updateDbEntry()
            GameScoreboardUtils.removeGameSidebar(player)
        }

        if (phase.isIngame()) {
            val ingamePhase: IngamePhase = phase as IngamePhase
            BlockoGame.instance.gameEntityHandler.clearEntitiesForTeam(gamePlayer.arenaId, gamePlayer.teamName!!)
            gamePlayer.actionTimeoutTimestamp = null

            for (currentGamePlayer: GamePlayer in this.currentPlayers.filter { !it.isAI }) {
                BlockoGame.instance.bossbarHandler.unregisterBossbar(currentGamePlayer.toBukkitInstance()!!, "timeoutBar")
            }

            ingamePhase.phaseMode = GamePhaseMode.DICE
            ingamePhase.setNextControllingTeam()
        }

        if (gamePlayer.teamName != null)
            BlockoGame.instance.gameTeamHandler.getTeamOfPlayer(this.id, player.uniqueId)?.quit(gamePlayer)

        this.invitedPlayers.removeIf { it == player.uniqueId }
        this.currentPlayers.removeIf { it.uuid == player.uniqueId }

        togglePlayerVisibility(player, false)

        if (this.currentPlayers.isEmpty()) this.phase.countdown?.cancel()

        if (this.phase.isIngame() && (this.currentPlayers.isEmpty() || this.currentPlayers.size == 1)) {
            reset(false)
            return
        }

        if (this.phase.isIdle() && this.phase.countdown != null && this.phase.countdown!!.isRunning) {
            this.phase.countdown!!.cancel()
            sendArenaMessage("blocko.countdown.idle.stopped_to_less_players")
        }

        if (this.arenaHost != null && this.arenaHost!!.uuid == player.uniqueId) {
            this.arenaHost = null
            this.arenaHost = findNewHost()

            if (this.arenaHost == null) {
                reset(false)
            } else {
                this.arenaHost?.translateMessage("blocko.arena.host_join")
            }
        }

        BlockoGame.instance.gameArenaSignHandler.updateArenaSign(this)
    }

    fun reset(shutdown: Boolean) {
        this.phase.countdown?.cancel()

        for (player: Player? in this.currentPlayers.filter { !it.isAI }.map { it.toBukkitInstance() }) {
            if (player == null) continue

            val lobbySpawn: LobbySpawn? = BlockoGame.instance.lobbySpawnHandler.lobbySpawn
            if (lobbySpawn != null && player.world.name != lobbySpawn.worldName)
                player.teleport(lobbySpawn.toBukkitInstance())

            GameScoreboardUtils.removeGameSidebar(player)
            BlockoGame.instance.bossbarHandler.clearBossbars(player)

            this.phase.clearPlayerInventory(player)
        }

        for (gamePlayer: GamePlayer in this.currentPlayers) {
            gamePlayer.actionTimeoutTimestamp = null
            gamePlayer.activeEntity = null
            gamePlayer.lastEntityPickRule = null

            val statsPlayer: StatsPlayer? = BlockoGame.instance.statsPlayerHandler.getStatsPlayer(gamePlayer.uuid)

            if (gamePlayer.isAI)
                BlockoGame.instance.statsPlayerHandler.cachedStatsPlayers.removeIf { it.uuid == gamePlayer.uuid }

            if (!gamePlayer.isAI && statsPlayer != null && !shutdown) statsPlayer.updateDbEntry()

            for (gameTeam: GameTeam in BlockoGame.instance.gameTeamHandler.gameTeams[this.id]) {
                gameTeam.quit(gamePlayer)
            }
        }

        for (gameTeam: GameTeam in BlockoGame.instance.gameTeamHandler.gameTeams[this.id]) {
            gameTeam.deactivated = false
        }

        val diceHandler: DiceHandler = BlockoGame.instance.diceHandler

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

        BlockoGame.instance.gameEntityHandler.clearEntitiesFromArena(this.id)

        for (gameField: GameField in BlockoGame.instance.gameFieldHandler.cachedGameFields[this.id]) {
            gameField.isTaken = false
            gameField.currentHolder = null
        }

        if (!this.phase.isIdle()) BlockoGame.instance.gamePhaseHandler.initIndexPhase(this)
        BlockoGame.instance.gameArenaSignHandler.updateArenaSign(this)
    }

    fun isGameOver(): Boolean {
        val finishedGamePlayers: List<GamePlayer> = this.currentPlayers.filter { it.getTeam().deactivated }.toList()
        val enoughGamePlayersFinished: Boolean = finishedGamePlayers.size == (this.currentPlayers.size - 1)
        return enoughGamePlayersFinished
    }

    fun isFull(): Boolean {
        return this.currentPlayers.size >= this.teamOptions.playerCount
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

    private fun togglePlayerVisibility(bukkitPlayer: Player, isJoin: Boolean) {
        for (currentPlayer: Player in Bukkit.getOnlinePlayers()) {
            if (isJoin) {
                val playersInSameArena: Boolean = (bukkitPlayer.getArena() != null && currentPlayer.getArena() != null) && (bukkitPlayer.getArena()!!.id == currentPlayer.getArena()!!.id)

                if (playersInSameArena) {
                    bukkitPlayer.showPlayer(BlockoGame.instance, currentPlayer)
                    currentPlayer.showPlayer(BlockoGame.instance, bukkitPlayer)
                } else {
                    bukkitPlayer.hidePlayer(BlockoGame.instance, currentPlayer)
                    currentPlayer.hidePlayer(BlockoGame.instance, bukkitPlayer)
                }
            } else {
                if (currentPlayer.getArena() != null) {
                    bukkitPlayer.hidePlayer(BlockoGame.instance, currentPlayer)
                    currentPlayer.hidePlayer(BlockoGame.instance, bukkitPlayer)
                } else {
                    bukkitPlayer.showPlayer(BlockoGame.instance, currentPlayer)
                    currentPlayer.showPlayer(BlockoGame.instance, bukkitPlayer)
                }
            }
        }
    }

}