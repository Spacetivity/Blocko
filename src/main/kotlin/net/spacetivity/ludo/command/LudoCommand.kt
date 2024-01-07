package net.spacetivity.ludo.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.arena.GameArenaHandler
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class LudoCommand : CommandExecutor {

    private val gameArenaHandler: GameArenaHandler = LudoGame.instance.gameArenaHandler!!

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            println("You must be a player to use this command!")
            return false
        }

        val player: Player = sender

        if (!player.hasPermission("ludo.command")) {
            player.sendMessage(Component.text("No perms! :("))
            return false
        }

        if (args.size == 2 && args[0].equals("arena", true) && args[1].equals("list", true)) {
            val cachedArenas: MutableSet<GameArena> = this.gameArenaHandler.cachedArenas

            if (cachedArenas.isEmpty()) {
                player.sendMessage(Component.text("No arenas found!", NamedTextColor.RED))
                return true
            }

            player.sendMessage(Component.text("All arenas: "))

            cachedArenas.forEach { gameArena: GameArena ->
                val arenaHostName: String = if (gameArena.arenaHost == null) "-/-" else gameArena.arenaHost!!.name
                val currentPlayerAmount: Int = gameArena.currentPlayers.size
                val maxPlayerAmount: Int = gameArena.maxPlayers
                player.sendMessage(Component.text("> #${gameArena.id} [JOIN] ($currentPlayerAmount/$maxPlayerAmount) (Host: ${arenaHostName})"))
            }

            return true
        }

        if (args.size == 2 && args[0].equals("arena", true) && args[1].equals("init", true)) {
            val creationStatus = this.gameArenaHandler.createArena(player.world.name, player.location)

            if (!creationStatus) {
                player.sendMessage(Component.text("Arena creation failed!", NamedTextColor.RED))
                return false
            }

            player.sendMessage(Component.text("Arena created!", NamedTextColor.GREEN))
            return true
        }

        sendUsage(player)
        return true
    }

    private fun sendUsage(player: Player) {
        // /ludo arena list
        // /ludo arena init
        // /ludo arena delete <name>
    }

}
