package net.spacetivity.blocko.command.api.subcommand

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class SpaceSubCommand(
    val length: Int = -1, // -1 means: value is not used
    val minLength: Int = -1,
    val maxLength: Int = -1,
    val parts: String,
    val permission: String = ""
)