package net.spacetivity.blocko.command.api.extension

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.command.api.SpaceCommand
import net.spacetivity.blocko.command.api.SpaceCommandSender
import net.spacetivity.blocko.command.api.SpaceMainCommandExecutor
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommand
import net.spacetivity.blocko.command.api.subcommand.SpaceSubCommandExecutor
import net.spacetivity.blocko.utils.DataTypeUtils

fun SpaceMainCommandExecutor.sendUsageFormatted(sender: SpaceCommandSender) {
    val translation = BlockoGame.instance.translationHandler.getSelectedTranslation()
    val subCommandParts: List<String> = findSubCommandParts()

    val mainCommandAnnotation = this::class.java.getAnnotation(SpaceCommand::class.java) ?: return

    val titlePlaceholder = Placeholder.parsed("title", mainCommandAnnotation.title)
    val usage = translation.usage(subCommandParts, titlePlaceholder)

    sender.sendMessage(usage.first)
    usage.second.forEach(sender::sendMessage)
}

inline fun <reified T> SpaceSubCommandExecutor.findArgument(audience: Audience, key: String, args: List<String>, dataType: Class<T>): T? {
    val translation = BlockoGame.instance.translationHandler.getSelectedTranslation()
    val subCommandAnnotation = this::class.java.getAnnotation(SpaceSubCommand::class.java) ?: return null
    val parts = subCommandAnnotation.parts.split(" ")

    val strippedList = parts.map { it.replace(Regex("[<>\\[\\]]"), "") }

    val placeholderIndex = strippedList.indexOf(key)
    val element = args.getOrNull(placeholderIndex) ?: return null

    var resultData: Pair<T?, String?> = Pair(null, null)
    DataTypeUtils.parseDataTypeNullable(dataType, element) { resultData = it }

    if (resultData.first == null) {
        audience.sendMessage(translation.line("blocko.command.argument_type_invalid", Placeholder.parsed("name", key), Placeholder.parsed("type", resultData.second!!)))
        return null
    }

    return resultData.first
}

inline fun <reified T> SpaceSubCommandExecutor.findArgument(audience: Audience, key: String, args: List<String>, dataType: Class<T>, result: (T?) -> Unit) {
    val subCommandAnnotation = this::class.java.getAnnotation(SpaceSubCommand::class.java) ?: return
    val parts = subCommandAnnotation.parts.split(" ")

    val strippedList = parts.map { it.replace(Regex("[<>\\[\\]]"), "") }

    val placeholderIndex = strippedList.indexOf(key)
    val element = args.getOrNull(placeholderIndex)

    if (element == null) {
        result(null)
        return
    }

    DataTypeUtils.parseDataType(audience, dataType, element, result)
}
