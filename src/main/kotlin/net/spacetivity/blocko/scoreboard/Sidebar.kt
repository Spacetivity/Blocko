package net.spacetivity.blocko.scoreboard

import net.kyori.adventure.text.Component
import net.spacetivity.blocko.utils.ScoreboardUtils
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard

class Sidebar(val viewer: Player, private val title: Component, val lines: Map<Int, Component>) {

    private var scoreboard: Scoreboard
    private var objective: Objective

    init {
        val scoreboardManager = Bukkit.getScoreboardManager()

        if (this.viewer.scoreboard == scoreboardManager.mainScoreboard)
            this.viewer.scoreboard = scoreboardManager.newScoreboard

        this.scoreboard = this.viewer.scoreboard

        val objectiveName = "display"

        if (this.scoreboard.getObjective(objectiveName) != null)
            this.scoreboard.getObjective(objectiveName)!!.unregister()

        this.objective = this.scoreboard.registerNewObjective(objectiveName, Criteria.DUMMY, this.title)
        this.objective.displaySlot = DisplaySlot.SIDEBAR

        initLines()
    }

    fun updateTitle(newTitle: Component) {
        this.objective.displayName(newTitle)
    }

    fun updateLine(lineId: Int, newLine: Component) {
        if (!hasLine(lineId)) return
        this.scoreboard.getTeam("x$lineId")?.prefix(newLine)
    }

    fun reset() {
        this.scoreboard.clearSlot(DisplaySlot.SIDEBAR)
    }

    private fun hasLine(lineId: Int): Boolean {
        return this.objective.getScore("§$lineId§7").isScoreSet
    }

    private fun initLines() {
        for (team in HashSet(this.scoreboard.teams)) team.unregister()

        for (line in this.lines.entries) {
            val lineId = line.key
            val team = ScoreboardUtils.registerScoreboardTeamWithContent(this.scoreboard, "x$lineId", Component.text(""), Component.text(""))

            val entryName = if (lineId < 10) "§$lineId§7" else "§${getColorCodeByName(lineId)}§7"
            if (team.hasEntry(entryName)) return

            team.prefix(line.value)
            team.addEntry(entryName)

            this.objective.getScore(entryName).score = lineId
        }
    }

    private fun getColorCodeByName(number: Int): String {
        return when (number) {
            10 -> "a"
            11 -> "b"
            12 -> "c"
            13 -> "d"
            14 -> "e"
            15 -> "f"
            else -> "z"
        }
    }

}