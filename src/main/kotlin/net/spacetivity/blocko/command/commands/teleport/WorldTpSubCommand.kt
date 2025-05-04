package net.spacetivity.blocko.command.commands.teleport

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.command.api.SpaceCommandSender
import net.spacetivity.blocko.command.api.extension.findArgument
import net.spacetivity.blocko.command.api.extension.generateSuggestions
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommand
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommandExecutor
import net.spacetivity.blocko.translation.translateMessage
import org.bukkit.Bukkit
import org.bukkit.WorldCreator
import org.bukkit.entity.Player

@SpaceSubCommand(length = 2, parts = "worldTp <world>")
class WorldTpSubCommand : SpaceSubCommandExecutor {

    override fun execute(sender: SpaceCommandSender, args: List<String>) {
        val player = sender.castTo(Player::class.java) ?: return
        val worldName = findArgument(player, "world", args, String::class.java) ?: return
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
    }

    override fun onTabComplete(sender: SpaceCommandSender, args: List<String>): List<String> {
        return buildList {
            addAll(generateSuggestions(args, 3, listOf(Pair(0, "arena"), Pair(1, "worldTp"))) {
                addAll(Bukkit.getWorlds().map { it.name })
            })
        }
    }

}