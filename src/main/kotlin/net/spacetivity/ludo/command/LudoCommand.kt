package net.spacetivity.ludo.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.inventory.api.SpaceInventoryProvider
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.arena.GameArenaHandler
import net.spacetivity.ludo.arena.GameArenaStatus
import net.spacetivity.ludo.arena.setup.GameArenaSetupData
import net.spacetivity.ludo.arena.setup.GameArenaSetupHandler
import net.spacetivity.ludo.command.api.CommandProperties
import net.spacetivity.ludo.command.api.LudoCommandExecutor
import net.spacetivity.ludo.command.api.LudoCommandSender
import net.spacetivity.ludo.entity.GameEntity
import net.spacetivity.ludo.entity.GameEntityHandler
import net.spacetivity.ludo.field.GameField
import net.spacetivity.ludo.field.GameFieldHandler
import net.spacetivity.ludo.field.TurnComponent
import net.spacetivity.ludo.utils.PathFace
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.entity.EntityType
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

        if (args.size == 1 && args[0].equals("arena", true)) {
            val inventory = SpaceInventoryProvider.api.inventoryHandler.getInventory(player, "arena_inv") ?: return
            inventory.open(player)
            return
        }

        //TODO: remove this (it's only for testing)
        if (args.size == 2 && args[0].equals("start", true)) {
            val arenaId: String = args[1]
            val gameArena: GameArena? = this.gameArenaHandler.getArena(arenaId)

            if (gameArena == null) {
                player.sendMessage(Component.text("Arena does not exist!"))
                return
            }

            if (gameArena.status != GameArenaStatus.READY) {
                player.sendMessage(Component.text("This arena is not ready to start a game!"))
                return
            }

            val gameEntityHandler: GameEntityHandler = LudoGame.instance.gameEntityHandler
            val gameFieldHandler: GameFieldHandler = LudoGame.instance.gameFieldHandler

            val entitiesFromArena: List<GameEntity> = gameEntityHandler.getEntitiesFromArena(arenaId)

            if (entitiesFromArena.isEmpty()) {
                val gameEntity = GameEntity(arenaId, "red", EntityType.VILLAGER)
                gameEntityHandler.gameEntities.put(arenaId, gameEntity)

                val firstField: GameField = gameFieldHandler.getField(arenaId, 0) ?: return
                gameEntity.spawn(firstField.getWorldPosition(0.0))
                player.sendMessage(Component.text("Spawned entity.", NamedTextColor.DARK_PURPLE))
            }

            val gameEntity: GameEntity = gameEntityHandler.gameEntities.get(arenaId).toList()[0]
            gameEntity.move(1, 0.0)
            player.sendMessage(Component.text("Moved entity.", NamedTextColor.LIGHT_PURPLE))

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

        if (args.size == 5 && args[0].equals("arena", true) && args[1].equals("setup", true) && args[2].equals("setTurn", true)) {
            checkSetupMode(player) { arenaSetupData: GameArenaSetupData ->
                val arenaId: String = arenaSetupData.arenaId
                val fieldId: Int = args[3].toInt()

                if (this.gameArenaHandler.cachedArenas.none { it.id == arenaId }) {
                    player.sendMessage(Component.text("Arena does not exist!"))
                    return@checkSetupMode
                }

                val pathFace: PathFace? = PathFace.entries.find { it.name.equals(args[4], true) }

                if (pathFace == null) {
                    player.sendMessage(Component.text("Available ${PathFace.entries.joinToString(", ") { it.name }}"))
                    return@checkSetupMode
                }

                val gameField: GameField? = arenaSetupData.gameFields.find { it.id == fieldId }

                if (gameField == null) {
                    player.sendMessage(Component.text("This field does not exist!"))
                    return@checkSetupMode
                }

                val turnComponent = TurnComponent(pathFace)

                gameField.turnComponent = turnComponent
                player.sendMessage(Component.text("You created a turning point for your field. (Direction: ${turnComponent.facing.name})"))
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
                "/ludo arena init",

                "/ludo arena setup start <arenaId>",
                "/ludo arena setup cancel",
                "/ludo arena setup finish",

                "/ludo arena setup setTurn <fieldId> <pathFace>",
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
            result.addAll(listOf("list", "init", "setup", "delete"))

        if (args.size == 2 && args[0].equals("worldTp", true))
            result.addAll(Bukkit.getWorlds().map { it.name })

        if (args.size == 3 && args[0].equals("arena", true) && args[1].equals("setup", true))
            result.addAll(listOf("start", "cancel", "finish", "setTurn", "addTeamSpawn"))

        if (args.size == 4 && args[0].equals("arena", true) && args[1].equals("setup", true) && args[2].equals("start", true))
            result.addAll(this.gameArenaHandler.cachedArenas.map { it.id })

        if (args.size == 5 && args[0].equals("arena", true) && args[1].equals("setup", true) && args[2].equals("setTurn", true))
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
