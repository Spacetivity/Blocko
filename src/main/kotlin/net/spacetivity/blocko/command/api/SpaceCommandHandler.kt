package net.spacetivity.blocko.command.api

class SpaceCommandHandler {

    private val commandExecutors: HashMap<String, SpaceCommandExecutor?> = HashMap()
    private val commands: MutableList<CommandProperties> = ArrayList()

    fun getCommands(): List<CommandProperties> {
        return commands
    }

    fun registerCommand(commandExecutor: SpaceCommandExecutor?): CommandProperties? {
        val command: CommandProperties =
            commandExecutor!!.javaClass.getAnnotation(CommandProperties::class.java) ?: return null

        if (!(command.permission.isEmpty() || command.permission.isBlank()))
            commandExecutors[command.name] = commandExecutor

        commands.add(command)
        return command
    }

    fun getCommandExecutor(name: String?): SpaceCommandExecutor? {
        return commandExecutors.values.stream().filter { handler: SpaceCommandExecutor? ->
            val properties = handler?.javaClass?.getAnnotation(CommandProperties::class.java)
            handler!!.javaClass.getAnnotation(CommandProperties::class.java) != null && name!!.isNotEmpty() && (properties?.name == name || properties?.aliases?.contains(
                name
            ) ?: false)
        }.findFirst().orElse(null)
    }

}