package net.spacetivity.blocko.command

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.arena.GameArenaHandler
import net.spacetivity.blocko.arena.GameArenaStatus
import net.spacetivity.blocko.arena.setup.GameArenaSetupData
import net.spacetivity.blocko.arena.setup.GameArenaSetupHandler
import net.spacetivity.blocko.command.api.CommandProperties
import net.spacetivity.blocko.command.api.SpaceCommandExecutor
import net.spacetivity.blocko.command.api.SpaceCommandSender
import net.spacetivity.blocko.extensions.translateMessage
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.entity.Player
import java.io.File


@CommandProperties("blocko", "blocko.command")
class BlockoCommand : SpaceCommandExecutor {

    private val arenaSetupHandler: GameArenaSetupHandler = BlockoGame.instance.gameArenaSetupHandler
    private val gameArenaHandler: GameArenaHandler = BlockoGame.instance.gameArenaHandler

    override fun execute(sender: SpaceCommandSender, args: List<String>) {
        if (!sender.isPlayer) return
        val player: Player = sender.castTo(Player::class.java)

        if (args.size == 2 && args[0].equals("arena", true) && args[1].equals("list", true)) {
            val cachedArenas: MutableList<GameArena> = this.gameArenaHandler.cachedArenas

            if (cachedArenas.isEmpty()) {
                player.translateMessage("blocko.command.blocko.no_arenas_found")
                return
            }

            player.translateMessage("blocko.command.blocko.arena_list.title")

            cachedArenas.forEach { gameArena: GameArena ->
                val currentPlayerAmount: Int = gameArena.currentPlayers.size
                val maxPlayerAmount: Int = gameArena.teamOptions.playerCount

                player.translateMessage("blocko.command.blocko.arena_list.line",
                    Placeholder.parsed("id", gameArena.id),
                    Placeholder.parsed("current_player_amount", currentPlayerAmount.toString()),
                    Placeholder.parsed("max_player_amount", maxPlayerAmount.toString()))
            }

            return
        }

        if (args.size == 2 && args[0].equals("arena", true) && args[1].equals("init", true)) {
            val creationStatus = this.gameArenaHandler.createArena(player.world.name, player.location)

            if (!creationStatus) {
                val maxArenaCount = BlockoGame.instance.globalConfigFile.gameArenaMaxParallelAmount
                player.translateMessage("blocko.command.blocko.arena_limit_reached", Placeholder.parsed("arena_limit", maxArenaCount.toString()))
                return
            }

            player.translateMessage("blocko.command.blocko.arena_created")
            return
        }

        if (args.size == 4 && args[0].equals("arena", true) && args[1].equals("setup", true) && args[2].equals("start", true)) {
            val arenaId: String = args[3]
            val gameArena: GameArena? = this.gameArenaHandler.getArena(arenaId)

            if (gameArena == null) {
                player.translateMessage("blocko.command.blocko.arena_not_exists", Placeholder.parsed("id", arenaId))
                return
            }

            if (gameArena.status != GameArenaStatus.CONFIGURATING) {
                player.translateMessage("blocko.command.blocko.arena_fully_configured")
                return
            }

            this.arenaSetupHandler.startSetup(player, arenaId)
            return
        }

        if (args.size == 3 && args[0].equals("arena", true) && args[1].equals("setup", true) && args[2].equals("cancel", true)) {
            checkSetupMode(player) { arenaSetupData: GameArenaSetupData ->
                if (this.gameArenaHandler.cachedArenas.none { it.id == arenaSetupData.arenaId }) {
                    player.translateMessage("blocko.command.blocko.arena_not_exists")
                    return@checkSetupMode
                }

                this.arenaSetupHandler.handleSetupEnd(player, false)
            }
            return
        }

        if (args.size == 3 && args[0].equals("arena", true) && args[1].equals("setup", true) && args[2].equals("finish", true)) {
            checkSetupMode(player) { arenaSetupData: GameArenaSetupData ->
                if (this.gameArenaHandler.cachedArenas.none { it.id == arenaSetupData.arenaId }) {
                    player.translateMessage("blocko.command.blocko.arena_not_exists")
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
                    player.translateMessage("blocko.command.blocko.arena_lacks_certain_team",
                        Placeholder.parsed("id", arenaSetupData.arenaId),
                        Placeholder.parsed("team_color", "<${arenaSetupData.gameTeams.find { it.name == teamName }!!.color.asHexString()}>"),
                        Placeholder.parsed("team_name", teamName))

                    return@checkSetupMode
                }

                this.arenaSetupHandler.addTeamSpawn(player, teamName, player.location)
            }
            return
        }

        if (args.size == 3 && args[0].equals("arena", true) && args[1].equals("delete", true)) {
            val arenaId: String = args[2]

            if (this.gameArenaHandler.cachedArenas.none { it.id == arenaId }) {
                player.translateMessage("blocko.command.blocko.arena_not_exists")
                return
            }

            this.gameArenaHandler.deleteArena(arenaId)
            player.translateMessage("blocko.command.blocko.arena_deleted")
            return
        }

        if (args.size == 2 && args[0].equals("worldTp", true)) {
            val worldName: String = args[1]
            val world: World? = Bukkit.getWorld(worldName)

            if (world == null) {
                val listFiles: Array<File> = Bukkit.getWorldContainer().listFiles() ?: return
                val worldFile: File? = listFiles.find { it.name.equals(worldName, true) }

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
        val player: Player = sender.castTo(Player::class.java)
        player.translateMessage("blocko.command.blocko.usage")
    }

    override fun onTabComplete(sender: SpaceCommandSender, args: List<String>): MutableList<String> {
        val result: MutableList<String> = mutableListOf()

        if (!sender.isPlayer) return result
        val player: Player = sender.castTo(Player::class.java)

        if (args.size == 1)
            result.addAll(listOf("arena", "worldTp"))

        if (args.size == 2 && args[0].equals("arena", true))
            result.addAll(listOf("list", "init", "setup", "delete"))

        if (args.size == 2 && args[0].equals("worldTp", true))
            result.addAll(Bukkit.getWorlds().map { it.name })

        if (args.size == 3 && args[0].equals("arena", true) && args[1].equals("setup", true))
            result.addAll(listOf("start", "cancel", "finish", "addTeamSpawn"))

        if (args.size == 4 && args[0].equals("arena", true) && args[1].equals("setup", true) && args[2].equals("start", true))
            result.addAll(this.gameArenaHandler.cachedArenas.map { it.id })

        if (args.size == 4 && args[0].equals("arena", true) && args[1].equals("setup", true) && args[2].equals("addTeamSpawn", true)) {
            val arenaSetupData = this.arenaSetupHandler.getSetupData(player.uniqueId) ?: return result
            result.addAll(arenaSetupData.gameTeams.map { it.name })
        }

        if (args.size == 3 && args[0].equals("arena", true) && (args[1].equals("delete", true)))
            result.addAll(this.gameArenaHandler.cachedArenas.map { it.id })

        return result
    }

    private fun checkSetupMode(player: Player, result: (GameArenaSetupData) -> Unit) {
        val arenaSetupData: GameArenaSetupData? = this.arenaSetupHandler.getSetupData(player.uniqueId)

        if (arenaSetupData == null) {
            player.translateMessage("blocko.setup.not_in_setup_mode")
            return
        }

        result.invoke(arenaSetupData)
    }

}
