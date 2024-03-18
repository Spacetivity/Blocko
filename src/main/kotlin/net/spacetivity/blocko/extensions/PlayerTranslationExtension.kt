package net.spacetivity.blocko.extensions

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.title.Title
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.translation.Translation
import org.bukkit.entity.Player

fun Player.translateMessage(key: String, vararg toReplace: TagResolver) {
    validateComponents(key, *toReplace).forEach { sendMessage(it) }
}

fun Player.translateActionBar(key: String, vararg toReplace: TagResolver) {
    validateComponents(key, *toReplace).forEach { sendActionBar(it) }
}

fun Player.translateTitle(key: String, vararg toReplace: TagResolver) {
    val titleParts: MutableList<Component> = validateComponents(key, *toReplace)
    if (titleParts.size > 2) throw UnsupportedOperationException("Title $key can only have two lines!")
    showTitle(Title.title(titleParts[0], titleParts[1]))
}

private fun validateComponents(key: String, vararg toReplace: TagResolver): MutableList<Component> {
    val selectedTranslation: Translation = BlockoGame.instance.translationHandler.getSelectedTranslation()
    val components: MutableList<Component> = mutableListOf()
    if (selectedTranslation.hasMultipleLines(key)) components.addAll(selectedTranslation.validateLines(key, *toReplace))
    else components.add(selectedTranslation.validateLine(key, *toReplace))
    return components
}