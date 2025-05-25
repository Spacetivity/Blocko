package net.spacetivity.blocko.arena.setup

import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.arena.id.ArenaId
import net.spacetivity.blocko.arena.setup.step.SetupStep
import net.spacetivity.blocko.arena.setup.step.impl.step.ScanBoardStep
import net.spacetivity.blocko.arena.setup.step.impl.step.SetTeamEntrancesStep
import net.spacetivity.blocko.arena.setup.step.impl.step.SetTeamPathsStep
import net.spacetivity.blocko.arena.setup.step.impl.step.SetTurningPointsStep
import net.spacetivity.blocko.utils.Constants
import java.time.Duration

class ArenaSetupSession(val arenaId: ArenaId) {

    lateinit var setupTool: SetupTool

    val timeoutTimestamp =
        if (Blocko.instance.setupConfigFile.setupSessionEndless) -1
        else System.currentTimeMillis() + Duration.ofMinutes(Blocko.instance.setupConfigFile.setupSessionTimeoutMinutes.toLong()).toMillis()

    var currentTeamName: String? = null
    val gameTeams = Constants.GAME_TEAMS

    val setupSteps = mutableMapOf(
        Pair(ScanBoardStep::class, ScanBoardStep()),
        Pair(SetTurningPointsStep::class, SetTurningPointsStep()),
        Pair(SetTeamEntrancesStep::class, SetTeamEntrancesStep()),
        Pair(SetTeamPathsStep::class, SetTeamPathsStep())
    )

    inline fun <reified T : SetupStep<*>> getSetupStep(): T? {
        return this.setupSteps.values.firstOrNull { it is T } as? T
    }

    fun getSetupStep(key: String): SetupStep<*>? {
        return this.setupSteps.values.find { it.key.equals(key, true) }
    }

    fun getActiveSetupStep(): SetupStep<*>? {
        var activeStep: SetupStep<*>? = null

        for (step in this.setupSteps.values) {
            if (!step.active) continue
            activeStep = step
        }

        return activeStep
    }

    fun setNextSetupStep(increase: Boolean): SetupStep<*> {
        val newStep = nextSetupStep(increase)

        getActiveSetupStep()?.active = false
        newStep.active = true

        return newStep
    }

    private fun nextSetupStep(increase: Boolean): SetupStep<*> {
        val initialStep = this.setupSteps.values.minBy { it.id }
        val activeSetupStep = getActiveSetupStep() ?: return initialStep

        val nextStepId = if (increase) activeSetupStep.id + 1 else activeSetupStep.id - 1
        val nextStep = this.setupSteps.values.find { it.id == nextStepId } ?: initialStep

        return nextStep
    }

}