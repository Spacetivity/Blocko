package net.spacetivity.blocko.command.commands.arena

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.arena.ArenaStatus
import net.spacetivity.blocko.arena.setup.getSetupSession
import net.spacetivity.blocko.arena.setup.step.impl.reset.ScanBoardResetData
import net.spacetivity.blocko.arena.setup.step.impl.reset.TeamPathResetData
import net.spacetivity.blocko.arena.setup.step.impl.step.ScanBoardStep
import net.spacetivity.blocko.arena.setup.step.impl.step.SetTeamEntrancesStep
import net.spacetivity.blocko.arena.setup.step.impl.step.SetTeamPathsStep
import net.spacetivity.blocko.arena.setup.step.impl.step.SetTurningPointsStep
import net.spacetivity.blocko.command.api.SpaceCommandSender
import net.spacetivity.blocko.command.api.extension.findArgument
import net.spacetivity.blocko.command.api.extension.generateSuggestions
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommand
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommandExecutor
import net.spacetivity.blocko.translation.translateMessage
import net.spacetivity.blocko.utils.HelperFunctions

@SpaceSubCommand(minLength = 5, maxLength = 7, parts = "arena setup resetStep <id> <step> [team] [index]")
class ArenaSetupResetStepSubCommand : SpaceSubCommandExecutor {

    override fun execute(sender: SpaceCommandSender, args: List<String>) {
        val player = sender.toPlayer()

        HelperFunctions.checkSetupMode(player) { setupSession ->
            val arenaIdAsString = findArgument(player, "id", args, String::class.java) ?: return@checkSetupMode
            val arenaId = Blocko.instance.arenaHandler.getArenaId(arenaIdAsString)
            val gameArena = arenaId?.let { Blocko.instance.arenaHandler.getArena(it) }

            if (arenaId == null || gameArena == null) {
                player.translateMessage("blocko.command.blocko.arena_not_exists", Placeholder.parsed("id", arenaIdAsString))
                return@checkSetupMode
            }

            if (gameArena.status != ArenaStatus.CONFIGURATING) {
                player.translateMessage("blocko.command.blocko.arena_fully_configured")
                return@checkSetupMode
            }

            val setupStepKey = findArgument(player, "step", args, String::class.java) ?: return@checkSetupMode
            val setupStep = setupSession.getSetupStep(setupStepKey)

            if (setupStep == null) {
                player.translateMessage("blocko.setup.reset.step.not_exist")
                return@checkSetupMode
            }

            var success = false

            when (setupStep) {
                is ScanBoardStep -> {
                    setupStep.reset(player, ScanBoardResetData(arenaId))
                    success = true
                }

                is SetTurningPointsStep, is SetTeamEntrancesStep -> {
                    setupStep.reset(player, null)
                    success = true
                }

                is SetTeamPathsStep -> {
                    val teamName = findArgument(player, "team", args, String::class.java)
                    val index = findArgument(player, "index", args, Int::class.java)
                    val optionalData = if (teamName != null && index != null) TeamPathResetData(teamName, index) else null

                    setupStep.reset(player, optionalData)
                    success = true
                }
            }

            if (!success) return@checkSetupMode
            player.translateMessage("blocko.setup.reset.step.success", Placeholder.parsed("name", setupStep.key))
        }
    }

    override fun onTabComplete(sender: SpaceCommandSender, args: List<String>): List<String> {
        val player = if (sender.isPlayer()) sender.toPlayer() else return emptyList()

        return buildList {
            addAll(generateSuggestions(args, 3, listOf(Pair(0, "arena"), Pair(1, "setup"))) { add("resetStep") })

            addAll(generateSuggestions(args, 4, listOf(Pair(0, "arena"), Pair(1, "setup"), Pair(2, "resetStep"))) {
                val setupSession = player.getSetupSession() ?: return@generateSuggestions
                add(setupSession.arenaId.value)
            })

            addAll(generateSuggestions(args, 5, listOf(Pair(0, "arena"), Pair(1, "setup"), Pair(2, "resetStep"))) {
                val setupSession = player.getSetupSession() ?: return@generateSuggestions
                addAll(setupSession.setupSteps.values.map { it.key })
            })

            addAll(generateSuggestions(args, 6, listOf(Pair(0, "arena"), Pair(1, "setup"), Pair(2, "resetStep"))) {
                val setupSession = player.getSetupSession() ?: return@generateSuggestions

                val setupStepKey = findArgument(player, "step", args, String::class.java) ?: return@generateSuggestions
                val setupStep = setupSession.getSetupStep(setupStepKey)
                if (setupStep !is SetTeamPathsStep) return@generateSuggestions

                addAll(setupSession.gameTeams.map { it.name })
            })
        }
    }

}