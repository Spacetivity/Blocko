package net.spacetivity.blocko.command.api

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class SpaceCommand (
    val name: String,
    val title: String,
    val permission: String = "",
    val aliases: Array<String> = []
)