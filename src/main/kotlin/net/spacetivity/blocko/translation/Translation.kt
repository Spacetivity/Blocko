package net.spacetivity.blocko.translation

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags
import java.text.MessageFormat
import java.util.*

class Translation(val name: String, val cachedMessages: MutableMap<String, String>) {

    private val defaultResolvers = setOf(
        StandardTags.gradient(),
        StandardTags.color(),
        StandardTags.decorations(),
        StandardTags.clickEvent(),
        StandardTags.hoverEvent()
    )

    private val miniMessage = MiniMessage.builder()
        .tags(TagResolver.builder().resolvers(defaultResolvers).build())
        .build()

    fun lineAsString(key: String, vararg args: Any): String {
        val message = cachedMessages[key] ?: return "$key not found..."
        return MessageFormat.format(message, *args)
    }

    fun line(key: String, vararg additionalResolvers: TagResolver): Component {
        val message = cachedMessages[key] ?: return errorComponent("$key not found...")
        return deserializeWithResolvers(message, *additionalResolvers)
    }

    fun lines(key: String, vararg additionalResolvers: TagResolver): List<Component> {
        val message = cachedMessages[key] ?: return listOf(errorComponent("$key not found..."))
        val lines = message.lines()
        if (lines.size <= 1) return listOf(errorComponent("$key is not a multiline message!"))
        return lines.map { line -> deserializeWithResolvers(line, *additionalResolvers) }
    }

    fun displayName(key: String, vararg additionalResolvers: TagResolver): Component {
        val message = cachedMessages[key] ?: return errorComponent("$key not found...")
        return deserializeWithResolvers("<!i>$message", *additionalResolvers)
    }

    fun lore(key: String, vararg additionalResolvers: TagResolver): List<Component> {
        val message = cachedMessages[key] ?: return listOf(errorComponent("$key not found..."))
        val lines = message.lines()
        return if (lines.size == 1) {
            listOf(deserializeWithResolvers("<!i>${lines[0]}", *additionalResolvers))
        } else {
            lines.map { line -> deserializeWithResolvers("<!i>$line", *additionalResolvers) }
        }
    }

    fun usage(subCommands: List<String>, vararg toReplace: TagResolver): Pair<Component, Set<Component>> {
        val title = line("blocko.command.usage.title", *toReplace)
        val subCommandLines = mutableSetOf<Component>()

        for (subCommandLine in subCommands) {
            subCommandLines.add(line("blocko.command.usage", Placeholder.parsed("command", subCommandLine), *toReplace))
        }

        return title to subCommandLines
    }

    private fun deserializeWithResolvers(message: String, vararg additionalResolvers: TagResolver): Component {
        // Build a combined resolver that includes defaults, additional ones, and extracted prefixes if needed.
        val combinedResolvers = mutableListOf<TagResolver>().apply {
            addAll(defaultResolvers)
            addAll(additionalResolvers)
            if (message.contains("<prefix")) add(extractPrefix(message))
        }
        val resolver = TagResolver.builder().resolvers(combinedResolvers).build()
        return MiniMessage.builder().tags(resolver).build().deserialize(message)
    }

    private fun errorComponent(text: String): Component {
        return miniMessage.deserialize("<red>$text")
    }

    private fun extractPrefix(content: String): TagResolver.Single {
        // Use regex to capture prefix from patterns like <prefix_myPrefix>
        val regex = Regex("<prefix_([^>]+)>")
        val matchResult = regex.find(content)
        return if (matchResult != null) {
            val rawPrefix = matchResult.groupValues[1].trimEnd { !it.isLetterOrDigit() }
            val validPrefix = rawPrefix.replaceFirstChar { it.uppercase(Locale.getDefault()) }
            val prefixTemplate = cachedMessages["blocko.prefix"] ?: "<prefix_text>"
            val prefixMessage = prefixTemplate.replace("<prefix_text>", validPrefix)
            Placeholder.component("prefix_$rawPrefix", miniMessage.deserialize(prefixMessage))
        } else if (content.contains("<prefix>")) {
            Placeholder.component("prefix", miniMessage.deserialize(cachedMessages["blocko.prefix.global"] ?: ""))
        } else {
            Placeholder.parsed("", "")
        }
    }

    fun hasMultipleLines(key: String): Boolean {
        return cachedMessages[key]?.contains("\n") == true
    }
}
