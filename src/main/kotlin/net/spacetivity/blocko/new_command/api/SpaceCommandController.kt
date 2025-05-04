package net.spacetivity.blocko.new_command.api

class SpaceCommandController {

    val commandExecutors = mutableMapOf<String, SpaceMainCommandExecutor>()
    val commands = mutableListOf<SpaceCommand>()

    fun registerCommand(executor: SpaceMainCommandExecutor): SpaceCommand {
        val commandAnnotation = executor::class.java.getAnnotation(SpaceCommand::class.java)
            ?: throw NullPointerException("Command class needs to have to @SpaceCommand annotation!")

        this.commandExecutors[commandAnnotation.name] = executor
        this.commands.add(commandAnnotation)

        return commandAnnotation
    }

    fun getCommandInitializer(name: String): SpaceMainCommandExecutor? {
        return this.commandExecutors.values.stream().filter { initializer ->
            val properties = initializer?.javaClass?.getAnnotation(SpaceCommand::class.java)
            initializer!!.javaClass.getAnnotation(SpaceCommand::class.java) != null && name.isNotEmpty() && (properties?.name == name || properties?.aliases?.contains(name) ?: false)
        }.findFirst().orElse(null)
    }

}