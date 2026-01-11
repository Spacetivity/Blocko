package net.spacetivity.blocko.scoreboard

import net.spacetivity.blocko.Blocko
import org.bukkit.Bukkit
import java.util.*

class SidebarHandler {

    private val cachedSidebars = mutableSetOf<Sidebar>()

    fun getSidebar(uuid: UUID): Sidebar? = this.cachedSidebars.find { it.viewer.uniqueId == uuid }

    fun registerSidebar(sidebar: Sidebar) {
        this.cachedSidebars.add(sidebar)
        Blocko.instance.gameFieldHighlightHandler.registerHighlightScoreboardTeams(sidebar.viewer)
    }

    fun unregisterSidebar(uuid: UUID) {
        val player = Bukkit.getPlayer(uuid)
        if (player != null) {
            Blocko.instance.gameFieldHighlightHandler.unregisterHighlightScoreboardTeams(player)
        }

        val sidebar = getSidebar(uuid) ?: return
        sidebar.reset()
        this.cachedSidebars.remove(sidebar)
    }

}