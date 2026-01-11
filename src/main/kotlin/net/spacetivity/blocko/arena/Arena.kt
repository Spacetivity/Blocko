package net.spacetivity.blocko.arena

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.arena.id.ArenaId
import net.spacetivity.blocko.phase.GamePhase
import net.spacetivity.blocko.phase.GamePhaseMode
import net.spacetivity.blocko.phase.impl.IngamePhase
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.player.getTeam
import net.spacetivity.blocko.stats.StatsPlayer
import net.spacetivity.blocko.team.GameTeamOptions
import net.spacetivity.blocko.translation.translateMessage
import net.spacetivity.blocko.utils.Constants
import net.spacetivity.blocko.utils.ScoreboardUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.Player
import java.util.*

class Arena(
    val id: ArenaId,
    val gameWorld: World,
    var status: ArenaStatus,
    var phase: GamePhase,
    val yLevel: Double,
    val location: Location,
) {

    var locked = false
    var waitForActualPlayers = true
    var teamOptions = GameTeamOptions.TWO_BY_ONE

    val currentPlayers = mutableSetOf<GamePlayer>()
    val spectatorPlayers = mutableSetOf<UUID>()

    var arenaHost: GamePlayer? = null

    val invitedPlayers = mutableSetOf<UUID>()

    init {
        if (this.status == ArenaStatus.READY) this.phase.start()
    }

    fun sendArenaMessage(key: String, vararg toReplace: TagResolver) {
        for (player in getAllPlayers()) player.translateMessage(key, *toReplace)
    }

    fun sendArenaSound(sound: Sound, volume: Float) {
        for (player in getAllPlayers()) player.playSound(player.location, sound, volume, 1F)
    }

    fun joinAsSpectator(player: Player) {
        if (this.spectatorPlayers.contains(player.uniqueId)) {
            player.translateMessage("blocko.arena.yourself_already_spectating_arena")
            return
        }

        if (!this.phase.isIngame()) {
            player.translateMessage("blocko.arena.not_spectatable")
            return
        }

        player.teleport(this.location)
        player.allowFlight = true
        player.isFlying = true

        this.spectatorPlayers.add(player.uniqueId)
        this.phase.setupPlayerInventory(player)

        player.translateMessage("blocko.arena.spectate_join")
        ScoreboardUtils.setGameSidebar(player)

        val ingamePhase = this.phase as IngamePhase
        val controllingTeam = ingamePhase.getControllingTeam() ?: return
        ScoreboardUtils.updateControllingTeamLine(this, controllingTeam)
        Blocko.instance.playerFormatHandler.setTablistFormatForAll()

        togglePlayerVisibility(player, PlayerVisibility.SPECTATING)
    }

    fun quitAsSpectator(player: Player) {
        if (!this.spectatorPlayers.contains(player.uniqueId)) return

        player.translateMessage("blocko.arena.spectate_quit")
        player.clearPhaseItems()

        val lobbySpawn = Blocko.instance.lobbySpawnHandler.lobbySpawn
        if (lobbySpawn != null && player.world.name != lobbySpawn.worldName) player.teleportAsync(lobbySpawn.toBukkitInstance())
            .thenAccept {
                togglePlayerVisibility(player, PlayerVisibility.IN_LOBBY)
                player.allowFlight = true
                player.isFlying = true
            }

        Blocko.instance.bossbarHandler.clearBossbars(player)
        ScoreboardUtils.removeSidebar(player)
        Blocko.instance.playerFormatHandler.setTablistFormatForAll()

        this.spectatorPlayers.remove(player.uniqueId)
    }

    fun join(uuid: UUID, isAI: Boolean): Boolean {
        val bukkitPlayer = Bukkit.getPlayer(uuid)
        val name = bukkitPlayer?.name ?: Blocko.instance.botNamesFile.botNames.random()

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

            val neededPlayerCount = if (this.waitForActualPlayers) this.teamOptions.playerCount else 1
            this.phase.countdown?.tryStartup({ playerCount -> playerCount == neededPlayerCount })

            togglePlayerVisibility(bukkitPlayer!!, PlayerVisibility.IN_ARENA)

            ScoreboardUtils.setGameSidebar(bukkitPlayer)
            Blocko.instance.playerFormatHandler.setTablistFormatForAll()
        } else {
            val aiStatsPlayer = StatsPlayer(uuid, 0, 0, 0, 0, 0)
            Blocko.instance.statsPlayerHandler.cachedStatsPlayers.add(aiStatsPlayer)
        }

        Blocko.instance.arenaSignHandler.updateArenaSign(this)
        return true
    }

    fun quit(player: Player) {
        if (this.currentPlayers.none { it.uuid == player.uniqueId }) return

        sendArenaMessage("blocko.arena.quit", Placeholder.parsed("name", player.name))
        player.clearPhaseItems()

        val lobbySpawn = Blocko.instance.lobbySpawnHandler.lobbySpawn
        if (lobbySpawn != null && player.world.name != lobbySpawn.worldName) player.teleportAsync(lobbySpawn.toBukkitInstance())
            .thenAccept {
                togglePlayerVisibility(player, PlayerVisibility.IN_LOBBY)
                player.allowFlight = true
                player.isFlying = true
            }

        Blocko.instance.bossbarHandler.clearBossbars(player)

        val gamePlayer = this.currentPlayers.find { it.uuid == player.uniqueId } ?: return

        if (Blocko.instance.diceHandler.dicingPlayers.containsKey(gamePlayer.uuid))
            Blocko.instance.diceHandler.dicingPlayers.remove(gamePlayer.uuid)

        if (!gamePlayer.isAI) {
            Blocko.instance.statsPlayerHandler.getStatsPlayer(player.uniqueId)?.updateDbEntry()
            ScoreboardUtils.removeSidebar(player)
        }

        if (phase.isIngame()) {
            val ingamePhase = phase as IngamePhase
            Blocko.instance.gameEntityHandler.clearEntitiesForTeam(gamePlayer.arenaId, gamePlayer.teamName!!)
            gamePlayer.actionTimeoutTimestamp = null

            for (currentGamePlayer in this.currentPlayers.filter { !it.isAI }) {
                Blocko.instance.bossbarHandler.unregisterBossbar(
                    currentGamePlayer.toBukkitInstance()!!,
                    Constants.TIMEOUT_BOSSBAR_NAME
                )
            }

            ingamePhase.phaseMode = GamePhaseMode.DICE
            ingamePhase.setNextControllingTeam()
        }

        if (gamePlayer.teamName != null)
            Blocko.instance.gameTeamHandler.getTeamOfPlayer(this.id, player.uniqueId)?.quit(gamePlayer)

        this.invitedPlayers.removeIf { it == player.uniqueId }
        this.currentPlayers.removeIf { it.uuid == player.uniqueId }

        togglePlayerVisibility(player, PlayerVisibility.IN_LOBBY)

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

        Blocko.instance.arenaSignHandler.updateArenaSign(this)
    }

    fun reset(shutdown: Boolean) {
        this.phase.countdown?.cancel()

        for (player in getAllPlayers()) {
            val lobbySpawn = Blocko.instance.lobbySpawnHandler.lobbySpawn
            if (lobbySpawn != null && player.world.name != lobbySpawn.worldName)
                player.teleport(lobbySpawn.toBukkitInstance())

            ScoreboardUtils.removeSidebar(player)
            Blocko.instance.bossbarHandler.clearBossbars(player)

            this.phase.clearPlayerInventory(player)
        }

        for (gamePlayer in this.currentPlayers) {
            gamePlayer.actionTimeoutTimestamp = null
            gamePlayer.activeEntity = null
            gamePlayer.lastEntityPickRule = null

            val statsPlayer = Blocko.instance.statsPlayerHandler.getStatsPlayer(gamePlayer.uuid)

            if (gamePlayer.isAI)
                Blocko.instance.statsPlayerHandler.cachedStatsPlayers.removeIf { it.uuid == gamePlayer.uuid }

            if (!gamePlayer.isAI && statsPlayer != null && !shutdown) statsPlayer.updateDbEntry()

            for (gameTeam in Blocko.instance.gameTeamHandler.gameTeams[this.id]) {
                gameTeam.quit(gamePlayer)
            }
        }

        for (gameTeam in Blocko.instance.gameTeamHandler.gameTeams[this.id]) {
            gameTeam.deactivated = false
        }

        val diceHandler = Blocko.instance.diceHandler

        for (currentPlayer in this.currentPlayers) {
            if (!diceHandler.dicingPlayers.containsKey(currentPlayer.uuid)) continue
            diceHandler.dicingPlayers.remove(currentPlayer.uuid)
        }

        this.invitedPlayers.clear()
        this.currentPlayers.clear()
        this.spectatorPlayers.clear()

        this.locked = false
        this.waitForActualPlayers = true
        this.teamOptions = GameTeamOptions.TWO_BY_ONE
        this.arenaHost = null

        Blocko.instance.gameEntityHandler.clearEntitiesFromArena(this.id)

        for (gameField in Blocko.instance.gameFieldHandler.cachedGameFields[this.id]) {
            gameField.isTaken = false
            gameField.currentHolder = null
        }

        if (!this.phase.isIdle()) Blocko.instance.gamePhaseHandler.initIndexPhase(this)
        Blocko.instance.arenaHandler.updateIngameArenaCache(this)
        Blocko.instance.arenaSignHandler.updateArenaSign(this)
    }

    fun sendArenaInvite(sender: GamePlayer, receiverName: String) {
        val gameArena = Blocko.instance.arenaHandler.getArena(sender.arenaId) ?: return

        if (!gameArena.phase.isIdle()) {
            sender.translateMessage("blocko.arena.game_already_started")
            return
        }

        val senderBukkitPlayer = sender.toBukkitInstance() ?: return
        val receiverBukkitPlayer = Bukkit.getPlayer(receiverName)

        if (receiverBukkitPlayer == null) {
            sender.translateMessage("blocko.utils.player_not_found")
            return
        }

        val receiverGamePlayer = receiverBukkitPlayer.toGamePlayerInstance()
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
        receiverBukkitPlayer.translateMessage(
            "blocko.arena.invite_received",
            Placeholder.parsed("name", senderBukkitPlayer.name),
            Placeholder.parsed("id", sender.arenaId.value)
        )

        Blocko.instance.arenaSignHandler.updateArenaSign(this)
    }

    fun getAllPlayers(): List<Player> {
        val players = this.spectatorPlayers.mapNotNull { Bukkit.getPlayer(it) }.toMutableList()
        players.addAll(this.currentPlayers.filter { !it.isAI }.mapNotNull { it.toBukkitInstance() })
        return players
    }

    fun isGameOver(): Boolean {
        val finishedGamePlayers = this.currentPlayers.filter { it.getTeam().deactivated }.toList()
        val enoughGamePlayersFinished = finishedGamePlayers.size == (this.currentPlayers.size - 1)
        return enoughGamePlayersFinished
    }

    fun isFull(): Boolean {
        return this.currentPlayers.size >= this.teamOptions.playerCount
    }

    private fun findNewHost(): GamePlayer? {
        val actualCurrentPlayers = this.currentPlayers.filter { !it.isAI }

        if (actualCurrentPlayers.isEmpty()) return null

        val newHostPlayer =
            if (this.arenaHost == null) actualCurrentPlayers.random()
            else actualCurrentPlayers.filter { it.uuid != this.arenaHost?.uuid }.random()

        return newHostPlayer
    }

    private fun togglePlayerVisibility(bukkitPlayer: Player, visibility: PlayerVisibility) {
        for (currentPlayer in Bukkit.getOnlinePlayers()) {
            when (visibility) {
                PlayerVisibility.IN_ARENA -> {
                    val playersInSameArena =
                        (bukkitPlayer.getArena() != null && currentPlayer.getArena() != null) && (bukkitPlayer.getArena()!!.id == currentPlayer.getArena()!!.id)

                    if (playersInSameArena) {
                        bukkitPlayer.showPlayer(Blocko.instance, currentPlayer)
                        currentPlayer.showPlayer(Blocko.instance, bukkitPlayer)
                    } else {
                        bukkitPlayer.hidePlayer(Blocko.instance, currentPlayer)
                        currentPlayer.hidePlayer(Blocko.instance, bukkitPlayer)
                    }
                }

                PlayerVisibility.IN_LOBBY -> {
                    if (currentPlayer.getArena() != null) {
                        bukkitPlayer.hidePlayer(Blocko.instance, currentPlayer)
                        currentPlayer.hidePlayer(Blocko.instance, bukkitPlayer)
                    } else {
                        bukkitPlayer.showPlayer(Blocko.instance, currentPlayer)
                        currentPlayer.showPlayer(Blocko.instance, bukkitPlayer)
                    }
                }

                PlayerVisibility.SPECTATING -> {
                    val isCurrentPlayerAlsoSpectator = this.spectatorPlayers.contains(currentPlayer.uniqueId)

                    if (isCurrentPlayerAlsoSpectator) {
                        bukkitPlayer.showPlayer(Blocko.instance, currentPlayer)
                        currentPlayer.showPlayer(Blocko.instance, bukkitPlayer)
                    } else {
                        bukkitPlayer.showPlayer(Blocko.instance, currentPlayer)
                        currentPlayer.hidePlayer(Blocko.instance, bukkitPlayer)
                    }
                }
            }
        }
    }

    enum class PlayerVisibility {
        IN_ARENA,
        IN_LOBBY,
        SPECTATING
    }

}