package net.spacetivity.blocko.command

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.ArenaHandler
import net.spacetivity.blocko.arena.ArenaStatus
import net.spacetivity.blocko.arena.setup.ArenaSetupHandler
import net.spacetivity.blocko.arena.setup.ArenaSetupSession
import net.spacetivity.blocko.arena.setup.step.impl.ScanBoardStep
import net.spacetivity.blocko.command.api.CommandProperties
import net.spacetivity.blocko.command.api.SpaceCommandExecutor
import net.spacetivity.blocko.command.api.SpaceCommandSender
import net.spacetivity.blocko.translation.translateMessage
import org.bukkit.Bukkit
import org.bukkit.WorldCreator
import org.bukkit.entity.Player


@CommandProperties("blocko", "blocko.command")
class BlockoCommand : SpaceCommandExecutor {

    private val arenaSetupHandler: ArenaSetupHandler = BlockoGame.instance.arenaSetupHandler
    private val arenaHandler: ArenaHandler = BlockoGame.instance.arenaHandler

    override fun execute(sender: SpaceCommandSender, args: List<String>) {
        if (!sender.isPlayer) return
        val player = sender.castTo(Player::class.java)

        if (args.size == 1 && args[0].equals("setLobbySpawn", true)) {
            BlockoGame.instance.lobbySpawnHandler.setLobbySpawn(player.location)
            player.translateMessage("blocko.command.blocko.lobby_spawn_set")
            return
        }

        if (args.size == 2 && args[0].equals("arena", true) && args[1].equals("list", true)) {
            val cachedArenas = this.arenaHandler.cachedArenas

            if (cachedArenas.isEmpty()) {
                player.translateMessage("blocko.command.blocko.no_arenas_found")
                return
            }

            player.translateMessage("blocko.command.blocko.arena_list.title")

            for (gameArena in cachedArenas) {
                val currentPlayerAmount = gameArena.currentPlayers.size
                val maxPlayerAmount = gameArena.teamOptions.playerCount

                player.translateMessage("blocko.command.blocko.arena_list.line",
                    Placeholder.parsed("id", gameArena.id.value),
                    Placeholder.parsed("status", gameArena.status.name),
                    Placeholder.parsed("current_player_amount", currentPlayerAmount.toString()),
                    Placeholder.parsed("max_player_amount", maxPlayerAmount.toString()))
            }

            return
        }

        if (args.size == 2 && args[0].equals("arena", true) && args[1].equals("init", true)) {
            val creationStatus = this.arenaHandler.createArena(player.world.name, player.location)

            if (!creationStatus) {
                val maxArenaCount = BlockoGame.instance.globalConfigFile.gameArenaMaxParallelAmount
                player.translateMessage("blocko.command.blocko.arena_limit_reached", Placeholder.parsed("arena_limit", maxArenaCount.toString()))
                return
            }

            player.translateMessage("blocko.command.blocko.arena_created")
            return
        }

        if (args.size == 4 && args[0].equals("arena", true) && args[1].equals("setup", true) && args[2].equals("start", true)) {
            val arenaIdAsString = args[3]
            val arenaId = arenaHandler.getArenaId(arenaIdAsString)
            val gameArena = arenaId?.let { arenaHandler.getArena(it) }

            if (arenaId == null || gameArena == null) {
                player.translateMessage("blocko.command.blocko.arena_not_exists", Placeholder.parsed("id", arenaIdAsString))
                return
            }

            if (gameArena.status != ArenaStatus.CONFIGURATING) {
                player.translateMessage("blocko.command.blocko.arena_fully_configured")
                return
            }

            this.arenaSetupHandler.startSetup(player, arenaId)
            return
        }

        if (args.size == 5 && args[0].equals("arena", true) && args[1].equals("setup", true) && args[2].equals("board", true) && args[3].equals("scan", true)) {
            checkSetupMode(player) { setupSession ->
                val arenaIdAsString = args[4]
                val arenaId = arenaHandler.getArenaId(arenaIdAsString)
                val gameArena = arenaId?.let { arenaHandler.getArena(it) }

                if (arenaId == null || gameArena == null) {
                    player.translateMessage("blocko.command.blocko.arena_not_exists", Placeholder.parsed("id", arenaIdAsString))
                    return@checkSetupMode
                }

                if (gameArena.status != ArenaStatus.CONFIGURATING) {
                    player.translateMessage("blocko.command.blocko.arena_fully_configured")
                    return@checkSetupMode
                }

                this.arenaSetupHandler.scanBoard(player)
            }
            return
        }

        if (args.size == 5 && args[0].equals("arena", true) && args[1].equals("setup", true) && args[2].equals("board", true) && args[3].equals("check", true)) {
            checkSetupMode(player) { setupSession ->
                val arenaIdAsString = args[4]
                val arenaId = arenaHandler.getArenaId(arenaIdAsString)
                val gameArena = arenaId?.let { arenaHandler.getArena(it) }

                if (arenaId == null || gameArena == null) {
                    player.translateMessage("blocko.command.blocko.arena_not_exists", Placeholder.parsed("id", arenaIdAsString))
                    return@checkSetupMode
                }

                if (gameArena.status != ArenaStatus.CONFIGURATING) {
                    player.translateMessage("blocko.command.blocko.arena_fully_configured")
                    return@checkSetupMode
                }

                val setupStep = setupSession.getSetupStep(ScanBoardStep::class) ?: return@checkSetupMode
                val missingScannerResults = setupStep.missingResults

                if (missingScannerResults.isEmpty()) {
                    player.translateMessage("blocko.setup.scanning_board.missing_fields.not_found")
                    return@checkSetupMode
                }

                player.translateMessage("blocko.setup.scanning_board.missing_fields.title")

                for ((scannerResult, amount) in missingScannerResults) {
                    val resultName = scannerResult.name
                        .replace('_', ' ')
                        .lowercase()
                        .replaceFirstChar { it.uppercase() }

                    player.translateMessage("blocko.setup.scanning_board.missing_fields.line",
                        Placeholder.parsed("result", resultName),
                        Placeholder.parsed("amount", amount.toString()))
                }
            }
            return
        }

        if (args.size == 3 && args[0].equals("arena", true) && args[1].equals("setup", true) && args[2].equals("cancel", true)) {
            checkSetupMode(player) { setupSession ->
                if (this.arenaHandler.cachedArenas.none { it.id == setupSession.arenaId }) {
                    player.translateMessage("blocko.command.blocko.arena_not_exists")
                    return@checkSetupMode
                }

                this.arenaSetupHandler.handleSetupEnd(player, false)
            }
            return
        }

        if (args.size == 3 && args[0].equals("arena", true) && args[1].equals("setup", true) && args[2].equals("finish", true)) {
            checkSetupMode(player) { setupSession ->
                if (this.arenaHandler.cachedArenas.none { it.id == setupSession.arenaId }) {
                    player.translateMessage("blocko.command.blocko.arena_not_exists")
                    return@checkSetupMode
                }

                this.arenaSetupHandler.handleSetupEnd(player, true)
            }
            return
        }

        if (args.size == 3 && args[0].equals("arena", true) && args[1].equals("delete", true)) {
            val arenaIdAsString = args[2]
            val arenaId = arenaHandler.getArenaId(arenaIdAsString)

            if (arenaId == null || this.arenaHandler.cachedArenas.none { it.id == arenaId }) {
                player.translateMessage("blocko.command.blocko.arena_not_exists", Placeholder.parsed("id", arenaIdAsString))
                return
            }

            this.arenaHandler.deleteArena(arenaId)
            player.translateMessage("blocko.command.blocko.arena_deleted")
            return
        }

        if (args.size == 2 && args[0].equals("worldTp", true)) {
            val worldName = args[1]
            val world = Bukkit.getWorld(worldName)

            if (world == null) {
                val listFiles = Bukkit.getWorldContainer().listFiles() ?: return
                val worldFile = listFiles.find { it.name.equals(worldName, true) }

                if (worldFile == null) {
                    player.translateMessage("blocko.command.blocko.world_does_not_exist")
                    return
                }

                WorldCreator(worldName).createWorld()
                player.translateMessage("blocko.command.blocko.world_loaded", Placeholder.parsed("world_name", worldName))
                return
            }

            player.teleportAsync(world.spawnLocation)
            player.translateMessage("blocko.command.blocko.world_teleported", Placeholder.parsed("world_name", worldName))
            return
        }

        sendUsage(sender)
        return
    }

    override fun sendUsage(sender: SpaceCommandSender) {
        if (!sender.isPlayer) return
        val player = sender.castTo(Player::class.java)
        player.translateMessage("blocko.command.blocko.usage")
    }

    override fun onTabComplete(sender: SpaceCommandSender, args: List<String>): MutableList<String> {
        val result = mutableListOf<String>()
        if (!sender.isPlayer) return result

        if (args.size == 1)
            result.addAll(listOf("setLobbySpawn", "arena", "worldTp"))

        if (args.size == 2 && args[0].equals("arena", true))
            result.addAll(listOf("list", "init", "setup", "delete"))

        if (args.size == 2 && args[0].equals("worldTp", true))
            result.addAll(Bukkit.getWorlds().map { it.name })

        if (args.size == 3 && args[0].equals("arena", true) && args[1].equals("setup", true))
            result.addAll(listOf("start", "cancel", "finish", "board"))

        if (args.size == 3 && args[0].equals("arena", true) && (args[1].equals("delete", true)))
            result.addAll(this.arenaHandler.cachedArenaIds.map { it.value })

        if (args.size == 4 && args[0].equals("arena", true) && args[1].equals("setup", true) && args[2].equals("start", true))
            result.addAll(this.arenaHandler.cachedArenaIds.map { it.value })

        if (args.size == 4 && args[0].equals("arena", true) && args[1].equals("setup", true) && args[2].equals("board", true))
            result.addAll(listOf("scan", "check"))

        if (args.size == 5 && args[0].equals("arena", true) && args[1].equals("setup", true) && args[2].equals("board", true) && (args[3].equals("scan", true) || args[3].equals("check", true)))
            result.addAll(this.arenaHandler.cachedArenaIds.map { it.value })

        return result
    }

    private fun checkSetupMode(player: Player, result: (ArenaSetupSession) -> Unit) {
        val setupSession = this.arenaSetupHandler.getSetupData(player.uniqueId)

        if (setupSession == null) {
            player.translateMessage("blocko.setup.not_in_setup_mode")
            return
        }

        result.invoke(setupSession)
    }

}
