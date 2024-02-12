package net.spacetivity.ludo.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.arena.GameArenaHandler
import net.spacetivity.ludo.arena.GameArenaStatus
import net.spacetivity.ludo.arena.setup.GameArenaSetupData
import net.spacetivity.ludo.arena.setup.GameArenaSetupHandler
import net.spacetivity.ludo.command.api.CommandProperties
import net.spacetivity.ludo.command.api.LudoCommandExecutor
import net.spacetivity.ludo.command.api.LudoCommandSender
import net.spacetivity.ludo.utils.PathFace
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.entity.Player
import java.io.File


@CommandProperties("ludo", "ludo.command")
class LudoCommand : LudoCommandExecutor {

    private val arenaSetupHandler: GameArenaSetupHandler = LudoGame.instance.gameArenaSetupHandler
    private val gameArenaHandler: GameArenaHandler = LudoGame.instance.gameArenaHandler

    override fun execute(sender: LudoCommandSender, args: List<String>) {
        if (!sender.isPlayer) {
            println("You must be a player to use this command!")
            return
        }

        val player: Player = sender.castTo(Player::class.java)

        if (!player.hasPermission("ludo.command")) {
            player.sendMessage(Component.text("No perms! :("))
            return
        }

        if (args.size == 2 && args[0].equals("arena", true) && args[1].equals("list", true)) {
            val cachedArenas: MutableList<GameArena> = this.gameArenaHandler.cachedArenas

            if (cachedArenas.isEmpty()) {
                player.sendMessage(Component.text("No arenas found!", NamedTextColor.RED))
                return
            }

            player.sendMessage(Component.text("All arenas: "))

            cachedArenas.forEach { gameArena: GameArena ->
                val arenaHostName: String = if (gameArena.arenaHost == null) "-/-" else gameArena.arenaHost!!.name
                val currentPlayerAmount: Int = gameArena.currentPlayers.size
                val maxPlayerAmount: Int = gameArena.maxPlayers
                player.sendMessage(Component.text("> ArenaId: ${gameArena.id} [JOIN] ($currentPlayerAmount/$maxPlayerAmount) (Host: ${arenaHostName})"))
            }

            return
        }

        if (args.size == 2 && args[0].equals("arena", true) && args[1].equals("init", true)) {
            val creationStatus = this.gameArenaHandler.createArena(player.world.name, player.location)

            if (!creationStatus) {
                player.sendMessage(Component.text("Arena creation failed!", NamedTextColor.RED))
                return
            }

            player.sendMessage(Component.text("Arena created!", NamedTextColor.GREEN))
            return
        }

        if (args.size == 2 && args[0].equals("arena", true) && (args[1].equals("join", true) || args[1].equals("quit", true))) {
            val isJoin: Boolean = args[1].equals("join", true)
            val arenaId: String = args[3]
            val gameArena: GameArena? = this.gameArenaHandler.getArena(arenaId)

            if (gameArena == null) {
                player.sendMessage(Component.text("Arena does not exist!"))
                return
            }

            if (gameArena.status != GameArenaStatus.READY) {
                player.sendMessage(Component.text("This arena is not ready to join!"))
                return
            }

            if (isJoin) gameArena.join(player)
            else gameArena.quit(player)
            return
        }

        if (args.size == 4 && args[0].equals("arena", true) && args[1].equals("setup", true) && args[2].equals("start", true)) {
            val arenaId: String = args[3]
            val gameArena: GameArena? = this.gameArenaHandler.getArena(arenaId)

            if (gameArena == null) {
                player.sendMessage(Component.text("Arena does not exist!"))
                return
            }

            if (gameArena.status != GameArenaStatus.CONFIGURATING) {
                player.sendMessage(Component.text("This arena is already fully configured!"))
                return
            }

            this.arenaSetupHandler.startSetup(player, arenaId)
            return
        }

        if (args.size == 3 && args[0].equals("arena", true) && args[1].equals("setup", true) && args[2].equals("cancel", true)) {
            checkSetupMode(player) { arenaSetupData: GameArenaSetupData ->
                if (this.gameArenaHandler.cachedArenas.none { it.id == arenaSetupData.arenaId }) {
                    player.sendMessage(Component.text("Arena does not exist!"))
                    return@checkSetupMode
                }

                this.arenaSetupHandler.handleSetupEnd(player, false)
            }
            return
        }

        if (args.size == 3 && args[0].equals("arena", true) && args[1].equals("setup", true) && args[2].equals("finish", true)) {
            checkSetupMode(player) { arenaSetupData: GameArenaSetupData ->
                if (this.gameArenaHandler.cachedArenas.none { it.id == arenaSetupData.arenaId }) {
                    player.sendMessage(Component.text("Arena does not exist!"))
                    return@checkSetupMode
                }

                this.arenaSetupHandler.handleSetupEnd(player, true)
            }
            return
        }

        if (args.size == 4 && args[0].equals("arena", true) && args[1].equals("setup", true) && args[2].equals("addTeamSpawn", true)) {
            checkSetupMode(player) { arenaSetupData: GameArenaSetupData ->
                val teamName: String = args[3]

                if (arenaSetupData.gameTeams.none { it.name.equals(teamName, true) }) {
                    player.sendMessage(Component.text("Arena ${arenaSetupData.arenaId} has no team called $teamName"))
                    return@checkSetupMode
                }

                this.arenaSetupHandler.addTeamSpawn(player, teamName, player.location)
            }
            return
        }

        if (args.size == 3 && args[0].equals("arena", true) && args[1].equals("delete", true)) {
            val arenaId: String = args[2]

            if (this.gameArenaHandler.cachedArenas.none { it.id == arenaId }) {
                player.sendMessage(Component.text("Arena does not exist!"))
                return
            }

            this.gameArenaHandler.deleteArena(arenaId)
            player.sendMessage(Component.text("Arena deleted!", NamedTextColor.YELLOW))
            return
        }

        if (args.size == 2 && args[0].equals("worldTp", true)) {
            val worldName: String = args[1]
            val world: World? = Bukkit.getWorld(worldName)

            if (world == null) {
                val listFiles: Array<File> = Bukkit.getWorldContainer().listFiles() ?: return
                val worldFile: File? = listFiles.find { it.name.equals(worldName, true) }

                if (worldFile == null) {
                    player.sendMessage(Component.text("This world does not exist!"))
                    return
                }

                WorldCreator(worldName).createWorld()
                player.sendMessage(Component.text("World $worldName loaded!"))
                player.sendMessage(Component.text("Please retype this command."))
                return
            }

            if (this.gameArenaHandler.cachedArenas.any { it.gameWorld.name == worldName }) {
                player.sendMessage(Component.text("This world has already a game arena!"))
                return
            }

            player.teleportAsync(world.spawnLocation)
            player.sendMessage(Component.text("Teleported to world $worldName."))
            return
        }

        sendUsage(sender)
        return
    }

    override fun sendUsage(sender: LudoCommandSender) {
        sender.sendMessages(
            arrayOf(
                "/ludo arena list",

                "/ludo arena join <arenaId",
                "/ludo arena quit <arenaId",

                "/ludo arena init",

                "/ludo arena setup start <arenaId>",
                "/ludo arena setup cancel",
                "/ludo arena setup finish",

                "/ludo arena setup addTeamSpawn <teamName>",

                "/ludo arena delete <arenaId>",
                "/ludo worldTp <worldName>",
            )
        )
    }

    override fun onTabComplete(sender: LudoCommandSender, args: List<String>): MutableList<String> {
        val result: MutableList<String> = mutableListOf()

        if (!sender.isPlayer) return result
        val player: Player = sender.castTo(Player::class.java)

        if (args.size == 1)
            result.addAll(listOf("arena", "worldTp"))

        if (args.size == 2 && args[0].equals("arena", true))
            result.addAll(listOf("list", "join", "quit", "init", "setup", "delete"))

        if (args.size == 2 && args[0].equals("worldTp", true))
            result.addAll(Bukkit.getWorlds().map { it.name })

        if (args.size == 3 && args[0].equals("arena", true) && args[1].equals("setup", true))
            result.addAll(listOf("start", "cancel", "finish", "addTeamSpawn"))

        if (args.size == 4 && args[0].equals("arena", true) && (args[1].equals("setup", true)
                    || args[1].equals("join", true)
                    || args[1].equals("quit", true)) && args[2].equals("start", true)
        )
            result.addAll(this.gameArenaHandler.cachedArenas.map { it.id })

        if (args.size == 5 && args[0].equals("arena", true) && args[1].equals("setup", true))
            result.addAll(PathFace.entries.map { it.name })

        if (args.size == 4 && args[0].equals("arena", true) && args[1].equals("setup", true) && args[2].equals("addTeamSpawn", true)) {
            val arenaSetupData = this.arenaSetupHandler.getSetupData(player.uniqueId) ?: return result
            result.addAll(arenaSetupData.gameTeams.map { it.name })
        }

        if (args.size == 3 && args[0].equals("arena", true) && (args[1].equals(
                "delete",
                true
            ) || args[1].equals("startSetup", true) || args[1].equals(
                "cancelSetup",
                true
            ) || args[1].equals("finishSetup", true))
        )
            result.addAll(this.gameArenaHandler.cachedArenas.map { it.id })

        return result
    }

    private fun checkSetupMode(player: Player, result: (GameArenaSetupData) -> Unit) {
        val arenaSetupData: GameArenaSetupData? = this.arenaSetupHandler.getSetupData(player.uniqueId)

        if (arenaSetupData == null) {
            player.sendMessage(Component.text("You are not in setup-mode!"))
            return
        }

        result.invoke(arenaSetupData)
    }

}
