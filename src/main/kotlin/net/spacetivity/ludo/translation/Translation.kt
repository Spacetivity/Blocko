package net.spacetivity.ludo.translation

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags
import java.text.MessageFormat
import java.util.*


class Translation(val name: String, val cachedMessages: MutableMap<String, String>) {

    private val defaultResolvers: MutableSet<TagResolver> = mutableSetOf(
        StandardTags.gradient(),
        StandardTags.color(),
        StandardTags.decorations(),
        StandardTags.clickEvent(),
        StandardTags.hoverEvent()
    )

    fun validateLineAsString(key: String, vararg toReplace: Any): String {
        val content = cachedMessages[key] ?: return "$key not found..."
        return MessageFormat.format(content, *toReplace)
    }

    fun validateLine(key: String, vararg toReplace: TagResolver): Component {
        val message = cachedMessages[key]
        val builder = MiniMessage.builder()

        if (message == null) return builder.tags(TagResolver.builder().resolver(StandardTags.color()).build()).build()
            .deserialize("<red>$key not found...")

        val tagBuilder: TagResolver.Builder = TagResolver.builder()
            .resolvers(this.defaultResolvers)
            .resolvers(*toReplace)

        if (message.contains("<prefix")) tagBuilder.resolver(extractPrefix(message))
        return builder.tags(tagBuilder.build()).build().deserialize(message)
    }

    fun validateLines(key: String, vararg toReplace: TagResolver): List<Component> {
        val message = cachedMessages[key]
        val builder = MiniMessage.builder()

        val build = builder.tags(TagResolver.builder().resolver(StandardTags.color()).build()).build()

        if (message == null) {
            return listOf(build.deserialize("<red>$key <red>not found..."))
        }

        if (!hasMultipleLines(key)) {
            return listOf(build.deserialize("<red>$key <red>is not a multiline message!"))
        }

        val lines = message.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val components: MutableList<Component> = ArrayList()

        for (line in lines) {
            val tagBuilder: TagResolver.Builder = TagResolver.builder()
                .resolvers(this.defaultResolvers)
                .resolvers(*toReplace)

            if (line.contains("<prefix")) tagBuilder.resolver(extractPrefix(line))
            components.add(builder.tags(tagBuilder.build()).build().deserialize(line))
        }

        return components
    }

    fun validateItemName(key: String, vararg toReplace: TagResolver): Component {
        val message = cachedMessages[key]
        val builder = MiniMessage.builder()

        if (message == null) return builder.tags(TagResolver.builder().resolver(StandardTags.color()).build()).build().deserialize("<red>$key not found...")

        val tagBuilder: TagResolver.Builder = TagResolver.builder()
            .resolvers(this.defaultResolvers)
            .resolvers(*toReplace)

        if (message.contains("<prefix")) tagBuilder.resolver(extractPrefix(message))
        return builder.tags(tagBuilder.build()).build().deserialize("<!i>$message")
    }

    fun validateItemLore(key: String, vararg toReplace: TagResolver): List<Component> {
        val message = cachedMessages[key]
        val builder = MiniMessage.builder()
        val build = builder.tags(TagResolver.builder().resolver(StandardTags.color()).build()).build()

        if (message == null) {
            return listOf(build.deserialize("<red>$key not found..."))
        }

        val components: MutableList<Component> = ArrayList()
        val tagBuilder: TagResolver.Builder = TagResolver.builder().resolvers(this.defaultResolvers).resolvers(*toReplace)

        if (!hasMultipleLines(key)) {
            if (message.contains("<prefix")) tagBuilder.resolver(extractPrefix(message))
            components.add(builder.tags(tagBuilder.build()).build().deserialize("<!i>$message"))
        } else {
            for (line in message.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                if (line.contains("<prefix")) tagBuilder.resolver(extractPrefix(line))
                components.add(builder.tags(tagBuilder.build()).build().deserialize("<!i>$line"))
            }
        }

        return components
    }

    private fun extractPrefix(content: String): TagResolver.Single {
        var placeholder: TagResolver.Single = Placeholder.parsed("", "")

        if (content.contains("<prefix_")) {
            val unformattedPrefixName = content.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0].split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
            val prefixName = unformattedPrefixName.substring(0, unformattedPrefixName.length - 1)
            val validPrefixName = prefixName.substring(0, 1).uppercase(Locale.getDefault()) + prefixName.substring(1)
            val prefix = cachedMessages["blocko.prefix"]!!.replace("<prefix_text>", validPrefixName)

            placeholder = Placeholder.component("prefix_$prefixName", MiniMessage.builder()
                .tags(TagResolver.builder()
                    .resolvers(this.defaultResolvers)
                    .build())
                .build()
                .deserialize(prefix))
        } else if (content.contains("<prefix>")) {
            placeholder = Placeholder.component(
                "prefix",
                MiniMessage.builder().tags(TagResolver.builder().resolvers(this.defaultResolvers).build())
                    .build().deserialize(cachedMessages["blocko.prefix.global"]!!)
            )
        }

        return placeholder
    }

    fun hasMultipleLines(key: String?): Boolean {
        return cachedMessages[key]!!.contains("\n")
    }


}