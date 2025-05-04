package net.spacetivity.blocko.new_command.api

import net.spacetivity.blocko.new_command.api.subcommand.SpaceSubCommand
import net.spacetivity.blocko.new_command.api.subcommand.SpaceSubCommandExecutor

abstract class SpaceMainCommandExecutor : SpaceCommandCompletable {

    private val commandAnnotation: SpaceCommand? = this::class.java.getAnnotation(SpaceCommand::class.java)

    val subCommandExecutors: MutableList<SpaceSubCommandExecutor> = mutableListOf()
    private val subCommands: MutableMap<SpaceSubCommand, SpaceSubCommandExecutor> = mutableMapOf()

    init {
        if (this.commandAnnotation == null) throw UnsupportedOperationException("SpaceCommand cannot be initiated! @SpaceCommand annotation is missing!")

        this.initSubCommands(this.subCommandExecutors)

        for (subCommand in subCommandExecutors) {
            val subCommandAnnotation = subCommand::class.java.getAnnotation(SpaceSubCommand::class.java)
                ?: throw UnsupportedOperationException("SpaceSubCommand cannot be initiated! @SpaceSubCommand annotation is missing!")

            this.subCommands[subCommandAnnotation] = subCommand
        }
    }

    abstract fun defaultExecute(sender: SpaceCommandSender)
    abstract fun onDefaultTabComplete(sender: SpaceCommandSender, args: List<String>): List<String>

    abstract fun initSubCommands(subCommandExecutors: MutableList<SpaceSubCommandExecutor>)

    fun findSubCommandParts(): List<String> {
        val subCommandParts = mutableListOf<String>()

        for ((subCommand, _) in this.subCommands) {
            subCommandParts.add("${this.commandAnnotation?.name} ${subCommand.parts}")
        }

        return subCommandParts
    }

    fun findValidSubCommandData(args: List<String>): Pair<SpaceSubCommandExecutor, SpaceSubCommand>? {
        var resultSubCommandExecutor: SpaceSubCommandExecutor? = null
        var subCommandResult: SpaceSubCommand? = null

        for ((subCommand, subCommandExecutor) in this.subCommands) {
            val subCommandParts = subCommand.parts.split(" ")

            val correctedSubCommandParts = subCommandParts.filter { !it.contains("<") && !it.contains(">") && !it.contains("[") && !it.contains("]") }

            if (!args.containsAll(correctedSubCommandParts)) continue
            if (subCommand.length != -1 && args.size != subCommand.length) continue
            if ((subCommand.minLength != -1 && subCommand.maxLength != -1) && (args.size < subCommand.minLength || args.size > subCommand.maxLength)) continue

            resultSubCommandExecutor = subCommandExecutor
            subCommandResult = subCommand
            break
        }

        return if (resultSubCommandExecutor == null || subCommandResult == null) null else resultSubCommandExecutor to subCommandResult
    }

}