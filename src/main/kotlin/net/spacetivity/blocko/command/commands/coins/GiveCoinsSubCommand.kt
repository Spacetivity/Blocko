package net.spacetivity.blocko.command.commands.coins

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.command.api.SpaceCommandSender
import net.spacetivity.blocko.command.api.extension.findArgument
import net.spacetivity.blocko.command.api.extension.generateSuggestions
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommand
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommandExecutor
import net.spacetivity.blocko.stats.addCoins
import net.spacetivity.blocko.translation.translateMessage
import org.bukkit.Bukkit

@SpaceSubCommand(length = 4, parts = "coins give <player> <amount>", permission = "blocko.command.admin")
class GiveCoinsSubCommand : SpaceSubCommandExecutor {

    override fun execute(sender: SpaceCommandSender, args: List<String>) {
        val player = sender.toPlayer()
        
        val targetPlayerName = findArgument(player, "player", args, String::class.java) ?: return
        val amount = findArgument(player, "amount", args, Int::class.java) ?: return
        
        val targetPlayer = Bukkit.getPlayer(targetPlayerName)
        
        if (targetPlayer == null) {
            player.translateMessage("blocko.utils.player_not_found")
            return
        }
        
        val statsPlayerHandler = Blocko.instance.statsPlayerHandler
        val statsPlayer = statsPlayerHandler.getStatsPlayer(targetPlayer.uniqueId)
        
        if (statsPlayer == null) {
            statsPlayerHandler.createOrLoadStatsPlayer(targetPlayer.uniqueId)
        }
        
        targetPlayer.addCoins(amount, false)
        statsPlayerHandler.getStatsPlayer(targetPlayer.uniqueId)?.updateDbEntry()
        
        player.translateMessage("blocko.command.coins.given",
            Placeholder.parsed("player", targetPlayerName),
            Placeholder.parsed("amount", amount.toString()))
    }

    override fun onTabComplete(sender: SpaceCommandSender, args: List<String>): List<String> {
        val player = if (sender.isPlayer()) sender.toPlayer() else return emptyList()

        return buildList {
            addAll(generateSuggestions(args, 1) { add("coins") })
            addAll(generateSuggestions(args, 2, listOf(Pair(0, "coins"))) { add("give") })
            addAll(generateSuggestions(args, 3, listOf(Pair(0, "coins"), Pair(1, "give"))) {
                addAll(Bukkit.getOnlinePlayers().map { it.name })
            })
            addAll(generateSuggestions(args, 4, listOf(Pair(0, "coins"), Pair(1, "give"), Pair(2, "<player>"))) {
                add("100")
                add("500")
                add("1000")
                add("5000")
            })
        }
    }

}
