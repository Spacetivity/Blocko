package net.spacetivity.blocko.new_command.api

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class SpaceCommand (
    val platform: SpaceCommandPlatform,
    val name: String,
    val title: String,
    val permission: String = "",
    val aliases: Array<String> = []
)