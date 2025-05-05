package net.spacetivity.blocko.arena.setup

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.id.ArenaId
import net.spacetivity.blocko.arena.setup.step.SetupStep
import net.spacetivity.blocko.arena.setup.step.impl.ScanBoardStep
import net.spacetivity.blocko.arena.setup.step.impl.SetTeamEntrancesStep
import net.spacetivity.blocko.arena.setup.step.impl.SetTeamPathsStep
import net.spacetivity.blocko.arena.setup.step.impl.SetTurningPointsStep
import net.spacetivity.blocko.utils.Constants
import java.time.Duration
import kotlin.reflect.KClass

class ArenaSetupSession(val arenaId: ArenaId, ) {

    lateinit var setupTool: SetupTool

    val timeoutTimestamp =
        if (BlockoGame.instance.setupConfigFile.setupSessionEndless) -1
        else System.currentTimeMillis() + Duration.ofMinutes(BlockoGame.instance.setupConfigFile.setupSessionTimeoutMinutes.toLong()).toMillis()

    var currentTeamName: String? = null
    val gameTeams = Constants.GAME_TEAMS

    val setupSteps = mutableMapOf(
        Pair(ScanBoardStep::class, ScanBoardStep()),
        Pair(SetTurningPointsStep::class, SetTurningPointsStep()),
        Pair(SetTeamEntrancesStep::class, SetTeamEntrancesStep()),
        Pair(SetTeamPathsStep::class, SetTeamPathsStep())
    )

    @Suppress("UNCHECKED_CAST")
    fun <T : SetupStep> getSetupStep(clazz: KClass<T>): T? {
        return this.setupSteps[clazz] as? T
    }

    fun getActiveSetupStep(): SetupStep? {
        var activeStep: SetupStep? = null

        for (step in this.setupSteps.values) {
            if (!step.active) continue
            activeStep = step
        }

        return activeStep
    }

    fun setNextSetupStep(increase: Boolean): SetupStep {
        val newStep = nextSetupStep(increase)

        getActiveSetupStep()?.active = false
        newStep.active = true

        return newStep
    }

    private fun nextSetupStep(increase: Boolean): SetupStep {
        val initialStep = this.setupSteps.values.minBy { it.id }
        val activeSetupStep = getActiveSetupStep() ?: return initialStep

        val nextStepId = if (increase) activeSetupStep.id + 1 else activeSetupStep.id - 1
        val nextStep = this.setupSteps.values.find { it.id == nextStepId } ?: initialStep

        return nextStep
    }

}