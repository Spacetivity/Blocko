package net.spacetivity.ludo.scoreboard

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player

class SidebarBuilder(private val viewer: Player) {

    private var title: Component = Component.text("Not found", NamedTextColor.RED)
    private val lines: LinkedHashMap<Int, Component> = linkedMapOf()

    fun setTitle(title: Component): SidebarBuilder {
        this.title = title
        return this
    }

    fun addLine(line: Component): SidebarBuilder {
        this.lines[this.lines.size] = line
        return this
    }

    fun addBlankLine(): SidebarBuilder {
        this.lines[this.lines.size] = Component.text(" ")
        return this
    }

    fun build(): Sidebar {
        val clonedKeys = HashSet(this.lines.keys)
        val correctedList = ArrayList(clonedKeys).apply { reverse() }
        val spaceCorrectedMap = modifyEmptyLines(this.lines)
        val keyIterator = correctedList.iterator()
        val keyCorrectedMap = LinkedHashMap<Int, Component>(spaceCorrectedMap.entries.associateBy({ keyIterator.next() }, { it.value }))
        return Sidebar(this.viewer, this.title, LinkedHashMap(keyCorrectedMap))
    }

    private fun modifyEmptyLines(lines: LinkedHashMap<Int, Component>): LinkedHashMap<Int, Component> {
        val modifiedLines: LinkedHashMap<Int, Component> = linkedMapOf()
        var emptyLineCount = 0

        for (entry: MutableMap.MutableEntry<Int, Component> in lines.entries) {
            val lineId: Int = entry.key
            val line: String = PlainTextComponentSerializer.plainText().serialize(entry.value)

            if (line.isBlank()) {
                val modifiedLine = Component.text(" ".repeat(emptyLineCount + 1))
                modifiedLines[lineId] = modifiedLine
                emptyLineCount++
            } else {
                modifiedLines[lineId] = entry.value
            }
        }

        return modifiedLines
    }

}