package net.spacetivity.blocko.command.commands.arena

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.ArenaStatus
import net.spacetivity.blocko.arena.setup.step.impl.ScanBoardStep
import net.spacetivity.blocko.command.api.SpaceCommandSender
import net.spacetivity.blocko.command.api.extension.findArgument
import net.spacetivity.blocko.command.api.extension.generateSuggestions
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommand
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommandExecutor
import net.spacetivity.blocko.translation.translateMessage
import net.spacetivity.blocko.utils.SetupUtils
import org.bukkit.entity.Player

@SpaceSubCommand(length = 5, parts = "arena setup board check <id>", permission = "blocko.command.admin")
class ArenaSetupBoardCheckSubCommand : SpaceSubCommandExecutor {

    override fun execute(sender: SpaceCommandSender, args: List<String>) {
        val player = sender.castTo(Player::class.java) ?: return
        SetupUtils.checkSetupMode(player) { setupSession ->
            val arenaIdAsString = findArgument(player, "id", args, String::class.java) ?: return@checkSetupMode
            val arenaId = BlockoGame.instance.arenaHandler.getArenaId(arenaIdAsString)
            val gameArena = arenaId?.let { BlockoGame.instance.arenaHandler.getArena(it) }

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
    }

    override fun onTabComplete(sender: SpaceCommandSender, args: List<String>): List<String> {
        return buildList {
            addAll(generateSuggestions(args, 3, listOf(Pair(0, "arena"), Pair(1, "setup"))) { add("board") })

            addAll(generateSuggestions(args, 4, listOf(Pair(0, "arena"), Pair(1, "setup"), Pair(2, "board"))) { add("check") })

            addAll(generateSuggestions(args, 5, listOf(Pair(0, "arena"), Pair(1, "setup"), Pair(2, "board"), Pair(3, "check"))) {
                addAll(BlockoGame.instance.arenaHandler.cachedArenaIds.map { it.value })
            })
        }
    }

}